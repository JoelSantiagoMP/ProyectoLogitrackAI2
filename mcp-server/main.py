"""
Servidor MCP LogiTrack IQ.

Pasarela autenticada (rol AGENTE) hacia la API REST de Spring Boot.
No accede a MySQL ni implementa reglas de negocio.
No expone herramientas para aprobar, cancelar ni recibir órdenes.
"""

from __future__ import annotations

import json
import os
import sys
from typing import Annotated, Any

import requests
from mcp.server.fastmcp import FastMCP
from mcp.server.transport_security import TransportSecuritySettings
from pydantic import Field

from schemas import (
    ACCION_PANEL_ITEM_JSON_SCHEMA,
    ALERTA_PANEL_ITEM_JSON_SCHEMA,
    parse_acciones,
    parse_alertas,
)

BASE_URL = os.getenv("LOGITRACK_API_BASE_URL", "http://localhost:8080").rstrip("/")
USERNAME = os.getenv("LOGITRACK_USERNAME", "agente_mcp")
PASSWORD = os.getenv("LOGITRACK_PASSWORD", "123456")

MCP_HOST = os.getenv("MCP_HOST", "127.0.0.1")
MCP_PORT = int(os.getenv("MCP_PORT", "8000"))
MCP_TRANSPORT = os.getenv("MCP_TRANSPORT", "stdio")
MCP_TRANSPORTS = ("stdio", "streamable-http", "sse")


def _env_bool(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


# Stateless + JSON: evita handshake SSE con sesión y el 400 "Missing session ID" en n8n.
MCP_STATELESS_HTTP = _env_bool("MCP_STATELESS_HTTP", True)
MCP_JSON_RESPONSE = _env_bool("MCP_JSON_RESPONSE", True)

mcp = FastMCP(
    "LogiTrack IQ",
    host=MCP_HOST,
    port=MCP_PORT,
    stateless_http=MCP_STATELESS_HTTP,
    json_response=MCP_JSON_RESPONSE,
    transport_security=TransportSecuritySettings(enable_dns_rebinding_protection=False),
)

_token: str | None = None


def _login() -> str:
    """Obtiene un JWT fresco contra POST /auth/login."""
    global _token
    response = requests.post(
        f"{BASE_URL}/auth/login",
        json={"username": USERNAME, "password": PASSWORD},
        timeout=30,
    )
    response.raise_for_status()
    data = response.json()
    _token = data.get("accessToken") or data.get("token")
    if not _token:
        raise RuntimeError("El backend no devolvió un token JWT en /auth/login")
    return _token


def _auth_headers() -> dict[str, str]:
    token = _token or _login()
    return {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }


def _request(method: str, path: str, **kwargs: Any) -> Any:
    """Ejecuta una petición HTTP inyectando JWT; reintenta una vez si expira (401)."""
    url = f"{BASE_URL}{path}"
    headers = _auth_headers()
    response = requests.request(method, url, headers=headers, timeout=30, **kwargs)

    if response.status_code == 401:
        global _token
        _token = None
        headers = _auth_headers()
        response = requests.request(method, url, headers=headers, timeout=30, **kwargs)

    if not response.ok:
        detail = response.text
        try:
            detail = json.dumps(response.json(), ensure_ascii=False)
        except Exception:
            pass
        raise RuntimeError(f"HTTP {response.status_code} {method} {path}: {detail}")

    if not response.content:
        return {"ok": True}

    try:
        return response.json()
    except ValueError:
        return {"raw": response.text}


def _as_json(payload: Any) -> str:
    return json.dumps(payload, ensure_ascii=False, indent=2)


@mcp.tool()
def consultar_stock_producto(producto_id: int) -> str:
    """Consulta el stock total y el desglose por bodega de un producto."""
    return _as_json(_request("GET", f"/api/productos/{producto_id}/stock"))


@mcp.tool()
def consultar_bodegas_criticas() -> str:
    """Lista bodegas con ocupación crítica (≥ 90 %)."""
    return _as_json(_request("GET", "/api/bodegas/criticas"))


@mcp.tool()
def consultar_productos_en_riesgo() -> str:
    """Lista productos en riesgo de quiebre de stock (punto de reorden / cobertura)."""
    return _as_json(_request("GET", "/api/productos/riesgo"))


@mcp.tool()
def consultar_kpis() -> str:
    """Obtiene los KPIs de inventario del sistema."""
    return _as_json(_request("GET", "/api/kpis"))


@mcp.tool()
def crear_orden_borrador(
    producto_id: int,
    proveedor_id: int,
    bodega_destino_id: int,
    cantidad: int,
    precio_unitario: float,
) -> str:
    """Crea una orden de compra en estado BORRADOR. No aprueba ni recibe la orden."""
    body = {
        "productoId": producto_id,
        "proveedorId": proveedor_id,
        "bodegaDestinoId": bodega_destino_id,
        "cantidad": cantidad,
        "precioUnitario": precio_unitario,
    }
    return _as_json(_request("POST", "/api/ordenes", json=body))


@mcp.tool()
def publicar_resumen(
    fecha: Annotated[
        str,
        Field(
            description="Fecha del resumen en formato YYYY-MM-DD (America/Bogota).",
            pattern=r"^\d{4}-\d{2}-\d{2}$",
        ),
    ],
    narrativa: Annotated[
        str,
        Field(
            description="Narrativa operativa basada en KPIs y riesgos reales (20-500 caracteres).",
            min_length=20,
            max_length=500,
        ),
    ],
    alertas: Annotated[
        list[dict[str, Any]],
        Field(
            description=(
                "Lista de alertas del panel. Cada alerta debe enlazar al menos un "
                "productoId, ordenId o bodegaId no nulo y existente."
            ),
            json_schema_extra={
                "type": "array",
                "items": ALERTA_PANEL_ITEM_JSON_SCHEMA,
            },
        ),
    ],
    acciones_sugeridas: Annotated[
        list[dict[str, Any]],
        Field(
            description=(
                "Lista de acciones sugeridas. Cada acción debe enlazar exactamente un "
                "ordenId, productoId o bodegaId no nulo y existente."
            ),
            json_schema_extra={
                "type": "array",
                "items": ACCION_PANEL_ITEM_JSON_SCHEMA,
            },
        ),
    ],
) -> str:
    """Publica el resumen del panel operativo (fecha YYYY-MM-DD, narrativa, alertas y acciones)."""
    alertas_validadas = parse_alertas(alertas)
    acciones_validadas = parse_acciones(acciones_sugeridas)
    body = {
        "fecha": fecha,
        "narrativa": narrativa,
        "alertas": [alerta.model_dump(by_alias=True) for alerta in alertas_validadas],
        "accionesSugeridas": [
            accion.model_dump(by_alias=True) for accion in acciones_validadas
        ],
    }
    return _as_json(_request("POST", "/api/panel/resumen", json=body))


def _run_streamable_http() -> None:
    """Levanta Streamable HTTP con Uvicorn (compatible con n8n httpStreamable)."""
    import uvicorn

    endpoint = mcp.settings.streamable_http_path
    print(
        f"MCP LogiTrack IQ — Streamable HTTP (Uvicorn)\n"
        f"  URL:      http://{MCP_HOST}:{MCP_PORT}{endpoint}\n"
        f"  Modo:     stateless={MCP_STATELESS_HTTP}, json_response={MCP_JSON_RESPONSE}\n"
        f"  Cliente:  n8n → serverTransport=httpStreamable",
        file=sys.stderr,
    )
    uvicorn.run(
        mcp.streamable_http_app(),
        host=MCP_HOST,
        port=MCP_PORT,
        log_level=mcp.settings.log_level.lower(),
    )


if __name__ == "__main__":
    if MCP_TRANSPORT not in MCP_TRANSPORTS:
        print(
            f"MCP_TRANSPORT inválido: {MCP_TRANSPORT!r}. "
            f"Valores permitidos: {', '.join(MCP_TRANSPORTS)}",
            file=sys.stderr,
        )
        sys.exit(1)

    if MCP_TRANSPORT == "streamable-http":
        _run_streamable_http()
    elif MCP_TRANSPORT == "sse":
        print(
            f"ADVERTENCIA: transporte SSE legacy en http://{MCP_HOST}:{MCP_PORT}{mcp.settings.sse_path}. "
            "Para n8n use MCP_TRANSPORT=streamable-http.",
            file=sys.stderr,
        )
        mcp.run(transport="sse")
    else:
        mcp.run(transport="stdio")
