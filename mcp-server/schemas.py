"""Esquemas Pydantic y JSON Schema inline para herramientas MCP (compatible con Gemini)."""

from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field

# Esquemas inline (sin $ref) exigidos por Gemini en arrays.
ALERTA_PANEL_ITEM_JSON_SCHEMA: dict[str, Any] = {
    "type": "object",
    "additionalProperties": False,
    "description": "Alerta del panel; debe enlazar al menos un ID no nulo existente.",
    "required": ["severidad", "titulo", "detalle", "productoId", "ordenId", "bodegaId"],
    "properties": {
        "severidad": {
            "type": "string",
            "enum": ["BAJA", "MEDIA", "ALTA"],
            "description": "Nivel de severidad: BAJA, MEDIA o ALTA.",
        },
        "titulo": {
            "type": "string",
            "description": "Título breve de la alerta.",
        },
        "detalle": {
            "type": "string",
            "description": "Detalle descriptivo de la alerta.",
        },
        "productoId": {
            "type": "integer",
            "nullable": True,
            "description": "ID del producto relacionado, o null.",
        },
        "ordenId": {
            "type": "integer",
            "nullable": True,
            "description": "ID de la orden relacionada, o null.",
        },
        "bodegaId": {
            "type": "integer",
            "nullable": True,
            "description": "ID de la bodega relacionada, o null.",
        },
    },
}

ACCION_PANEL_ITEM_JSON_SCHEMA: dict[str, Any] = {
    "type": "object",
    "additionalProperties": False,
    "description": "Acción sugerida; debe enlazar exactamente un ID no nulo existente.",
    "required": ["tipo", "descripcion", "ordenId", "productoId", "bodegaId"],
    "properties": {
        "tipo": {
            "type": "string",
            "enum": ["REVISAR_ORDEN", "REVISAR_PRODUCTO", "REVISAR_BODEGA"],
            "description": "Tipo: REVISAR_ORDEN, REVISAR_PRODUCTO o REVISAR_BODEGA.",
        },
        "descripcion": {
            "type": "string",
            "description": "Texto descriptivo de la acción sugerida.",
        },
        "ordenId": {
            "type": "integer",
            "nullable": True,
            "description": "ID de la orden a revisar, o null.",
        },
        "productoId": {
            "type": "integer",
            "nullable": True,
            "description": "ID del producto a revisar, o null.",
        },
        "bodegaId": {
            "type": "integer",
            "nullable": True,
            "description": "ID de la bodega a revisar, o null.",
        },
    },
}


class AlertaPanelInput(BaseModel):
    """Alerta del resumen del panel operativo (validación en runtime)."""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    severidad: Literal["BAJA", "MEDIA", "ALTA"] = Field(
        description="Nivel de severidad: BAJA, MEDIA o ALTA."
    )
    titulo: str = Field(description="Título breve de la alerta.")
    detalle: str = Field(description="Detalle descriptivo de la alerta.")
    producto_id: int | None = Field(
        default=None,
        alias="productoId",
        description="ID del producto relacionado, o null si no aplica.",
    )
    orden_id: int | None = Field(
        default=None,
        alias="ordenId",
        description="ID de la orden relacionada, o null si no aplica.",
    )
    bodega_id: int | None = Field(
        default=None,
        alias="bodegaId",
        description="ID de la bodega relacionada, o null si no aplica.",
    )


class AccionPanelInput(BaseModel):
    """Acción sugerida del resumen del panel operativo (validación en runtime)."""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    tipo: Literal["REVISAR_ORDEN", "REVISAR_PRODUCTO", "REVISAR_BODEGA"] = Field(
        description="Tipo de acción: REVISAR_ORDEN, REVISAR_PRODUCTO o REVISAR_BODEGA."
    )
    descripcion: str = Field(description="Texto descriptivo de la acción sugerida.")
    orden_id: int | None = Field(
        default=None,
        alias="ordenId",
        description="ID de la orden a revisar, o null si no aplica.",
    )
    producto_id: int | None = Field(
        default=None,
        alias="productoId",
        description="ID del producto a revisar, o null si no aplica.",
    )
    bodega_id: int | None = Field(
        default=None,
        alias="bodegaId",
        description="ID de la bodega a revisar, o null si no aplica.",
    )


def parse_alertas(raw_alertas: list[dict[str, Any]]) -> list[AlertaPanelInput]:
    return [AlertaPanelInput.model_validate(alerta) for alerta in raw_alertas]


def parse_acciones(raw_acciones: list[dict[str, Any]]) -> list[AccionPanelInput]:
    return [AccionPanelInput.model_validate(accion) for accion in raw_acciones]
