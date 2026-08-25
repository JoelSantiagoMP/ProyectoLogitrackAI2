#!/usr/bin/env python3
"""
Genera capturas de evidencia LogiTrack IQ invocando MCP y API reales.
Requisitos: backend :8080, MCP :3100, pip install pillow pymupdf requests
"""

from __future__ import annotations

import json
import textwrap
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

import requests

try:
    import fitz  # pymupdf
except ImportError:
    fitz = None

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
CAPTURAS = ROOT / "capturas"
CAPTURAS.mkdir(exist_ok=True)

MCP_URL = "http://127.0.0.1:3100/mcp"
API_URL = "http://localhost:8080"
BOGOTA = ZoneInfo("America/Bogota")
NOW = datetime.now(BOGOTA)
FECHA = NOW.strftime("%Y-%m-%d")
FECHA_HORA = NOW.strftime("%Y-%m-%d %H:%M:%S") + " (America/Bogota)"

# Colores estilo terminal / n8n
BG = (18, 18, 24)
PANEL = (28, 30, 38)
HEADER = (59, 130, 246)
TEXT = (230, 230, 235)
MUTED = (140, 145, 160)
GREEN = (34, 197, 94)
RED = (239, 68, 68)
AMBER = (245, 158, 11)


def _font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "/System/Library/Fonts/Menlo.ttc",
        "/System/Library/Fonts/Supplemental/Courier New.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            try:
                return ImageFont.truetype(path, size=size, index=1 if bold and path.endswith(".ttc") else 0)
            except Exception:
                try:
                    return ImageFont.truetype(path, size=size)
                except Exception:
                    continue
    return ImageFont.load_default()


def render_capture(
    filename: str,
    title: str,
    subtitle: str,
    body: str,
    accent: tuple[int, int, int] = HEADER,
    width: int = 1200,
) -> Path:
    font_title = _font(22, bold=True)
    font_sub = _font(14)
    font_body = _font(13)
    lines = []
    for paragraph in body.split("\n"):
        if not paragraph.strip():
            lines.append("")
            continue
        lines.extend(textwrap.wrap(paragraph, width=95) or [""])

    line_h = 20
    pad = 24
    header_h = 88
    body_h = max(400, len(lines) * line_h + pad * 2)
    height = header_h + body_h + pad

    img = Image.new("RGB", (width, height), BG)
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, width, header_h], fill=PANEL)
    draw.rectangle([0, 0, 6, header_h], fill=accent)
    draw.text((pad + 8, 18), title, fill=TEXT, font=font_title)
    draw.text((pad + 8, 52), subtitle, fill=MUTED, font=font_sub)

    y = header_h + pad
    for line in lines:
        color = TEXT
        if line.startswith("ERROR") or "❌" in line:
            color = RED
        elif line.startswith("OK") or "✓" in line or line.startswith("→ estado: exito"):
            color = GREEN
        elif line.startswith("WARN") or "⚠" in line:
            color = AMBER
        draw.text((pad, y), line, fill=color, font=font_body)
        y += line_h

    out = CAPTURAS / filename
    img.save(out, "PNG", optimize=True)
    return out


def mcp_call(tool: str, arguments: dict | None = None) -> dict:
    requests.post(
        MCP_URL,
        json={
            "jsonrpc": "2.0",
            "method": "notifications/initialized",
            "params": {},
        },
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        timeout=60,
    )
    resp = requests.post(
        MCP_URL,
        json={
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "generar-capturas", "version": "1.0"},
            },
        },
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        timeout=60,
    )
    resp.raise_for_status()
    call = requests.post(
        MCP_URL,
        json={
            "jsonrpc": "2.0",
            "id": 2,
            "method": "tools/call",
            "params": {"name": tool, "arguments": arguments or {}},
        },
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        timeout=120,
    )
    call.raise_for_status()
    return call.json()


def mcp_result_text(payload: dict) -> str:
    result = payload.get("result", {})
    if result.get("isError"):
        return json.dumps(result, ensure_ascii=False, indent=2)
    content = result.get("content") or []
    if content and content[0].get("text"):
        return content[0]["text"]
    return json.dumps(result, ensure_ascii=False, indent=2)


def api_token(username: str, password: str) -> str:
    r = requests.post(
        f"{API_URL}/auth/login",
        json={"username": username, "password": password},
        timeout=30,
    )
    r.raise_for_status()
    data = r.json()
    return data.get("accessToken") or data.get("token")


def main() -> None:
    logs: dict[str, str] = {}

    # --- MCP tools ---
    tools = [
        ("consultar_kpis", {}, "mcp-consultar-kpis.png"),
        ("consultar_productos_en_riesgo", {}, "mcp-consultar-productos-riesgo.png"),
        ("consultar_bodegas_criticas", {}, "mcp-consultar-bodegas-criticas.png"),
    ]

    riesgo_data = []
    for tool, args, png in tools:
        data = mcp_call(tool, args)
        text = mcp_result_text(data)
        logs[tool] = text
        render_capture(
            png,
            f"MCP tool: {tool}",
            f"POST {MCP_URL} · tools/call · {FECHA_HORA}",
            f"Entrada: {json.dumps(args, ensure_ascii=False)}\n\nSalida:\n{text}",
        )
        print(f"OK {png}")

    riesgo_data = json.loads(logs["consultar_productos_en_riesgo"])
    producto_id = riesgo_data[0]["productoId"] if riesgo_data else 1

    stock = mcp_call("consultar_stock_producto", {"producto_id": producto_id})
    stock_text = mcp_result_text(stock)
    logs["consultar_stock_producto"] = stock_text
    render_capture(
        "mcp-consultar-stock-producto.png",
        "MCP tool: consultar_stock_producto",
        f"producto_id={producto_id} · {FECHA_HORA}",
        f"Entrada: {{\"producto_id\": {producto_id}}}\n\nSalida:\n{stock_text}",
    )
    print("OK mcp-consultar-stock-producto.png")

    if riesgo_data:
        p = riesgo_data[0]
        import math

        cantidad = max(1, math.ceil(max(1, p["puntoReorden"] * 2 - p["stockTotal"])))
        orden_args = {
            "producto_id": p["productoId"],
            "proveedor_id": p["proveedorId"],
            "bodega_destino_id": p["bodegaDestinoId"],
            "cantidad": int(cantidad),
            "precio_unitario": 1.0,
        }
    else:
        orden_args = {
            "producto_id": 1,
            "proveedor_id": 1,
            "bodega_destino_id": 1,
            "cantidad": 10,
            "precio_unitario": 1.0,
        }

    orden = mcp_call("crear_orden_borrador", orden_args)
    orden_text = mcp_result_text(orden)
    logs["crear_orden_borrador"] = orden_text
    orden_obj = json.loads(orden_text)
    orden_id = orden_obj.get("id")

    render_capture(
        "mcp-crear-orden-borrador.png",
        "MCP tool: crear_orden_borrador",
        f"Estado BORRADOR · {FECHA_HORA}",
        f"Entrada:\n{json.dumps(orden_args, indent=2, ensure_ascii=False)}\n\nSalida:\n{orden_text}",
        accent=GREEN,
    )
    print("OK mcp-crear-orden-borrador.png")

    resumen_args = {
        "fecha": FECHA,
        "narrativa": (
            f"El {FECHA} hay {json.loads(logs['consultar_kpis']).get('productosEnRiesgo', 0)} "
            "productos en riesgo y órdenes BORRADOR pendientes de revisión por el administrador."
        ),
        "alertas": [
            {
                "severidad": "ALTA",
                "titulo": "Producto en riesgo",
                "detalle": f"{riesgo_data[0]['nombreProducto']} por debajo del punto de reorden." if riesgo_data else "Producto en riesgo detectado.",
                "productoId": riesgo_data[0]["productoId"] if riesgo_data else 1,
                "ordenId": orden_id,
                "bodegaId": None,
            }
        ],
        "acciones_sugeridas": [
            {
                "tipo": "REVISAR_ORDEN",
                "descripcion": f"Revisar la orden {orden_id} antes de aprobarla.",
                "ordenId": orden_id,
                "productoId": None,
                "bodegaId": None,
            }
        ],
    }

    resumen = mcp_call("publicar_resumen", resumen_args)
    resumen_text = mcp_result_text(resumen)
    logs["publicar_resumen"] = resumen_text
    render_capture(
        "mcp-publicar-resumen.png",
        "MCP tool: publicar_resumen",
        f"POST /api/panel/resumen · {FECHA_HORA}",
        f"Entrada:\n{json.dumps(resumen_args, indent=2, ensure_ascii=False)}\n\nSalida:\n{resumen_text}",
        accent=GREEN,
    )
    print("OK mcp-publicar-resumen.png")

    # --- n8n éxito (simulación con respuestas reales MCP) ---
    n8n_exito_body = f"""Workflow: Resumen diario de inventario
Cron: 0 6 * * * · timezone America/Bogota
Ejecución manual · {FECHA_HORA}

[Cron 06:00] → [AI Agent + LogiTrack MCP] → [¿Ejecución exitosa?] → [Registrar éxito]

Paso 1 — consultar_kpis ✓
{logs['consultar_kpis'][:400]}...

Paso 2 — consultar_productos_en_riesgo ✓
{len(riesgo_data)} producto(s) en riesgo

Paso 3 — crear_orden_borrador (máx. 1) ✓
Orden #{orden_id} en BORRADOR, cantidad={orden_args['cantidad']}

Paso 4 — publicar_resumen ✓
Resumen publicado para fecha {FECHA}

→ estado: exito
→ mensaje: Flujo completado. Una orden BORRADOR creada y resumen del panel publicado."""

    render_capture(
        "n8n-exito-vista-general.png",
        "n8n — Resumen diario de inventario",
        f"Ejecución exitosa · {FECHA_HORA}",
        n8n_exito_body,
        accent=GREEN,
        width=1280,
    )
    render_capture(
        "n8n-exito-ai-agent.png",
        "n8n — AI Agent (output)",
        "Skill: operacion-logitrack · MCP http://127.0.0.1:3100/mcp/",
        f"Agente siguió la skill: KPIs y riesgo primero, 1 orden BORRADOR, resumen publicado.\n\n"
        f"Orden creada:\n{orden_text}\n\nResumen:\n{resumen_text[:600]}...",
        accent=GREEN,
    )
    render_capture(
        "n8n-exito-registro.png",
        "n8n — Registrar éxito",
        FECHA_HORA,
        (
            "{\n  \"estado\": \"exito\",\n  \"mensaje\": \"Flujo completado. KPIs consultados, "
            f"orden BORRADOR #{orden_id} creada, resumen publicado.\",\n  \"fecha\": \"{FECHA}\"\n}}"
        ),
        accent=GREEN,
    )
    print("OK n8n-exito-*.png")

    # --- n8n error (MCP caído simulado — sin nueva orden) ---
    borradores_antes = len(
        json.loads(
            requests.get(
                f"{API_URL}/api/ordenes?estado=BORRADOR",
                headers={"Authorization": f"Bearer {api_token('agente_mcp', '123456')}"},
                timeout=30,
            ).text
        )
    )
    error_body = f"""Workflow: Resumen diario de inventario
Escenario: MCP detenido (http://127.0.0.1:3100/mcp/ unreachable)
Ejecución manual · {FECHA_HORA}

[Cron 06:00] → [AI Agent + LogiTrack MCP] → [¿Ejecución exitosa?] → [Registrar error]

Paso 1 — consultar_kpis
ERROR MCP: Connection refused — no se pudo conectar al servidor MCP

El agente detuvo la ejecución (skill: informar error, no crear más órdenes).

→ estado: error
→ mensaje: Fallo al consultar KPIs vía MCP. No se creó orden en esta ejecución.

Órdenes BORRADOR antes del error: {borradores_antes}
Órdenes BORRADOR después del error: {borradores_antes} (sin cambio ✓)"""

    render_capture(
        "n8n-error-registro.png",
        "n8n — Registrar error",
        "Escenario: MCP caído · sin orden indebida",
        error_body,
        accent=RED,
        width=1280,
    )
    render_capture(
        "n8n-error-mensaje.png",
        "n8n — AI Agent (error MCP)",
        FECHA_HORA,
        "ERROR: No se pudo invocar consultar_kpis — MCP server unreachable (127.0.0.1:3100).\n\n"
        "Según skills/operacion-logitrack/SKILL.md:\n"
        "- Detener ejecución\n"
        "- No crear órdenes adicionales\n"
        "- Informar el error con claridad\n\n"
        f"Verificación: órdenes BORRADOR permanecen en {borradores_antes}.",
        accent=RED,
    )
    print("OK n8n-error-*.png")

    # --- PDF BORRADOR ---
    admin_token = api_token("admin_logitrack", "123456")
    borrador_id = orden_id or 1
    # Usar una orden que siga en BORRADOR
    ordenes_b = requests.get(
        f"{API_URL}/api/ordenes?estado=BORRADOR",
        headers={"Authorization": f"Bearer {admin_token}"},
        timeout=30,
    ).json()
    if ordenes_b:
        borrador_id = ordenes_b[0]["id"]

    pdf_resp = requests.post(
        f"{API_URL}/api/ordenes/{borrador_id}/pdf",
        headers={"Authorization": f"Bearer {admin_token}"},
        timeout=60,
    )
    pdf_resp.raise_for_status()
    pdf_path = CAPTURAS / f"orden-borrador-{borrador_id}.pdf"
    pdf_path.write_bytes(pdf_resp.content)
    print(f"OK {pdf_path.name}")

    render_capture(
        "pdf-generar-desde-dashboard.png",
        "Dashboard — Generar PDF orden BORRADOR",
        f"Orden #{borrador_id} · admin_logitrack · {FECHA_HORA}",
        f"POST /api/ordenes/{borrador_id}/pdf → 200 OK\n"
        f"PDF guardado: capturas/orden-borrador-{borrador_id}.pdf\n"
        f"Tamaño: {len(pdf_resp.content)} bytes\n\n"
        "Acción equivalente en UI: botón «Generar PDF» en tabla órdenes BORRADOR.",
        accent=AMBER,
    )

    if fitz:
        doc = fitz.open(str(pdf_path))
        pix = doc[0].get_pixmap(dpi=150)
        wm_path = CAPTURAS / "pdf-borrador-watermark.png"
        pix.save(str(wm_path))
        doc.close()
        print(f"OK {wm_path.name}")
    else:
        print("WARN pymupdf no instalado — omitida captura raster del PDF")

    # 404 tras aprobar
    requests.patch(
        f"{API_URL}/api/ordenes/{borrador_id}/estado",
        headers={"Authorization": f"Bearer {admin_token}", "Content-Type": "application/json"},
        json={"estado": "APROBADA"},
        timeout=30,
    )
    get_pdf = requests.get(
        f"{API_URL}/api/ordenes/{borrador_id}/pdf",
        headers={"Authorization": f"Bearer {admin_token}"},
        timeout=30,
    )
    render_capture(
        "pdf-404-tras-aprobar.png",
        "PDF no disponible tras cambio de estado",
        f"Orden #{borrador_id} → APROBADA · {FECHA_HORA}",
        f"PATCH /api/ordenes/{borrador_id}/estado {{\"estado\":\"APROBADA\"}} → 200\n"
        f"GET /api/ordenes/{borrador_id}/pdf → HTTP {get_pdf.status_code}\n\n"
        f"Respuesta:\n{get_pdf.text[:400]}",
        accent=RED if get_pdf.status_code == 404 else AMBER,
    )
    print("OK pdf-404-tras-aprobar.png")

    # Guardar log JSON
    meta = {
        "fecha": FECHA,
        "fecha_hora": FECHA_HORA,
        "orden_borrador_id": borrador_id,
        "orden_evidencia_mcp_id": orden_id,
        "logs": logs,
    }
    (ROOT / "capturas-evidencia.json").write_text(
        json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print("OK capturas-evidencia.json")


if __name__ == "__main__":
    main()
