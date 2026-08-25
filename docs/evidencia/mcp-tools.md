# Evidencia — 6 tools MCP

Servidor: `http://localhost:3100/mcp` (Streamable HTTP)  
Usuario backend: `agente_mcp` / `123456`  
**Fecha de generación:** 2026-08-25 18:15:32 (America/Bogota)

Regenerar capturas:

```bash
# Backend :8080 y MCP :3100 activos
cd mcp-server && source .venv/bin/activate
pip install pillow pymupdf requests
python ../docs/evidencia/generar_capturas.py
```

---

## 1. `consultar_kpis`

**API:** `GET /api/kpis`

| Campo | Valor |
| --- | --- |
| Fecha prueba | 2026-08-25 |
| Captura | [capturas/mcp-consultar-kpis.png](capturas/mcp-consultar-kpis.png) |

**Entrada (parámetros):** ninguno

**Salida real (fragmento):**

```json
{
  "calculadoEn": "2026-08-25T18:15:32.654907-05:00",
  "productosEnQuiebre": 0,
  "productosEnRiesgo": 2,
  "ordenesPorAprobar": { "cantidad": 2, "montoTotal": 740082.0 },
  "movimientosAyer": { "entrada": 1, "salida": 0, "transferencia": 0 }
}
```

---

## 2. `consultar_productos_en_riesgo`

**API:** `GET /api/productos/riesgo`

| Campo | Valor |
| --- | --- |
| Fecha prueba | 2026-08-25 |
| Captura | [capturas/mcp-consultar-productos-riesgo.png](capturas/mcp-consultar-productos-riesgo.png) |

**Salida real:** 2 productos — Resma Papel A4 (id 10), Toner Laser Negro (id 11), con `puntoReorden`, `diasCobertura`, `bodegaDestinoId`.

---

## 3. `consultar_bodegas_criticas`

**API:** `GET /api/bodegas/criticas`

| Campo | Valor |
| --- | --- |
| Fecha prueba | 2026-08-25 |
| Captura | [capturas/mcp-consultar-bodegas-criticas.png](capturas/mcp-consultar-bodegas-criticas.png) |

**Salida real:** `[]` (ninguna bodega ≥ 90 % en el momento de la prueba).

---

## 4. `consultar_stock_producto`

**API:** `GET /api/productos/{id}/stock`

| Campo | Valor |
| --- | --- |
| Fecha prueba | 2026-08-25 |
| `producto_id` usado | 10 (Resma Papel A4) |
| Captura | [capturas/mcp-consultar-stock-producto.png](capturas/mcp-consultar-stock-producto.png) |

**Salida real:**

```json
{
  "productoId": 10,
  "stockTotal": 8,
  "porBodega": [{ "bodegaId": 1, "nombre": "Bodega Principal Bucaramanga", "cantidad": 8 }]
}
```

---

## 5. `crear_orden_borrador`

**API:** `POST /api/ordenes`

| Campo | Valor |
| --- | --- |
| Fecha prueba | 2026-08-25 |
| Captura | [capturas/mcp-crear-orden-borrador.png](capturas/mcp-crear-orden-borrador.png) |

**Entrada usada:**

```json
{
  "producto_id": 10,
  "proveedor_id": 1,
  "bodega_destino_id": 4,
  "cantidad": 82,
  "precio_unitario": 1.0
}
```

**Salida:** orden `#4` con `"estado": "BORRADOR"`, `total` calculado en servidor.

**Confirmado:** no existe tool para aprobar/recibir/cancelar.

---

## 6. `publicar_resumen`

**API:** `POST /api/panel/resumen`

| Campo | Valor |
| --- | --- |
| Fecha prueba | 2026-08-25 |
| Captura | [capturas/mcp-publicar-resumen.png](capturas/mcp-publicar-resumen.png) |

**Verificado:** `GET /api/panel/resumen` devuelve el JSON publicado con narrativa, alertas y acción `REVISAR_ORDEN` (orden 4).

---

## Resumen de cumplimiento

| Tool | Probada | Captura | OK |
| --- | --- | --- | --- |
| `consultar_kpis` | [x] | [x] | [x] |
| `consultar_productos_en_riesgo` | [x] | [x] | [x] |
| `consultar_bodegas_criticas` | [x] | [x] | [x] |
| `consultar_stock_producto` | [x] | [x] | [x] |
| `crear_orden_borrador` | [x] | [x] | [x] |
| `publicar_resumen` | [x] | [x] | [x] |

Log completo: [capturas-evidencia.json](capturas-evidencia.json)
