# Evidencia — 6 tools MCP

Servidor: `http://localhost:3100/mcp` (Streamable HTTP)  
Usuario backend: `agente_mcp` / `123456`

Verificación rápida de que el MCP responde:

```bash
curl -s -X POST http://localhost:3100/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"evidencia","version":"1.0"}}}'
```

Alternativa: invocar cada tool desde **Cursor MCP**, **n8n AI Agent** o el inspector MCP de tu cliente.

---

## 1. `consultar_kpis`

**API:** `GET /api/kpis`

| Campo | Valor |
| --- | --- |
| Fecha prueba | `_YYYY-MM-DD_` |
| Captura | `capturas/mcp-consultar-kpis.png` |

**Entrada (parámetros):** ninguno

**Salida esperada (fragmento):**

```json
{
  "calculadoEn": "2026-08-25T06:00:00-05:00",
  "ocupacionPorBodega": [{ "bodegaId": 1, "nombre": "...", "porcentaje": 92.5 }],
  "productosEnQuiebre": 1,
  "productosEnRiesgo": 2,
  "ordenesPorAprobar": { "cantidad": 1, "montoTotal": 45000.0 },
  "movimientosAyer": { "entrada": 2, "salida": 3, "transferencia": 1 }
}
```

**Log / notas:**

```text
_pegar respuesta real o error_
```

---

## 2. `consultar_productos_en_riesgo`

**API:** `GET /api/productos/riesgo`

| Campo | Valor |
| --- | --- |
| Fecha prueba | `_YYYY-MM-DD_` |
| Captura | `capturas/mcp-consultar-productos-riesgo.png` |

**Entrada:** ninguno

**Salida esperada (por elemento):** `productoId`, `nombreProducto`, `proveedorId`, `stockTotal`, `consumoDiarioPromedio`, `puntoReorden`, `diasCobertura`, `estadoCobertura`, `bodegaDestinoId`

**Log / notas:**

```text

```

---

## 3. `consultar_bodegas_criticas`

**API:** `GET /api/bodegas/criticas`

| Campo | Valor |
| --- | --- |
| Fecha prueba | `_YYYY-MM-DD_` |
| Captura | `capturas/mcp-consultar-bodegas-criticas.png` |

**Entrada:** ninguno

**Salida esperada:** lista con bodegas donde `porcentaje >= 90`

**Log / notas:**

```text

```

---

## 4. `consultar_stock_producto`

**API:** `GET /api/productos/{id}/stock`

| Campo | Valor |
| --- | --- |
| Fecha prueba | `_YYYY-MM-DD_` |
| `producto_id` usado | `_ej. 1_` |
| Captura | `capturas/mcp-consultar-stock-producto.png` |

**Entrada:** `producto_id` (entero existente en BD)

**Salida esperada:**

```json
{
  "productoId": 1,
  "stockTotal": 42,
  "porBodega": [{ "bodegaId": 1, "nombre": "...", "cantidad": 10 }]
}
```

**Log / notas:**

```text

```

---

## 5. `crear_orden_borrador`

**API:** `POST /api/ordenes`

| Campo | Valor |
| --- | --- |
| Fecha prueba | `_YYYY-MM-DD_` |
| Captura | `capturas/mcp-crear-orden-borrador.png` |

**Entrada de ejemplo:**

```json
{
  "producto_id": 1,
  "proveedor_id": 1,
  "bodega_destino_id": 1,
  "cantidad": 15,
  "precio_unitario": 1000.0
}
```

**Salida esperada:** orden con `"estado": "BORRADOR"` y `total` calculado en servidor.

**Confirmar:** no existe tool para aprobar/recibir/cancelar.

**Log / notas:**

```text

```

---

## 6. `publicar_resumen`

**API:** `POST /api/panel/resumen`

| Campo | Valor |
| --- | --- |
| Fecha prueba | `_YYYY-MM-DD_` (debe ser hoy en America/Bogota) |
| Captura | `capturas/mcp-publicar-resumen.png` |

**Entrada (vía parámetros MCP):**

- `fecha`: `YYYY-MM-DD`
- `narrativa`: 20–500 caracteres
- `alertas[]`: severidad `BAJA|MEDIA|ALTA`, al menos un ID existente
- `acciones_sugeridas[]`: tipo `REVISAR_ORDEN|REVISAR_PRODUCTO|REVISAR_BODEGA`, exactamente un ID

**Verificar después:** `GET /api/panel/resumen` devuelve el JSON publicado.

**Log / notas:**

```text

```

---

## Resumen de cumplimiento

| Tool | Probada | Captura | OK |
| --- | --- | --- | --- |
| `consultar_kpis` | [ ] | [ ] | [ ] |
| `consultar_productos_en_riesgo` | [ ] | [ ] | [ ] |
| `consultar_bodegas_criticas` | [ ] | [ ] | [ ] |
| `consultar_stock_producto` | [ ] | [ ] | [ ] |
| `crear_orden_borrador` | [ ] | [ ] | [ ] |
| `publicar_resumen` | [ ] | [ ] | [ ] |
