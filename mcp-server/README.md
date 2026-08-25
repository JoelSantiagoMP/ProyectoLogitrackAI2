# Servidor MCP LogiTrack

Pasarela MCP hacia la API REST de Spring Boot. Autentica un usuario con rol `AGENTE` y reenvía llamadas HTTP. **No** accede a MySQL y **no** implementa reglas de negocio.

## Herramientas (solo 6)

| Tool | API |
| --- | --- |
| `consultar_stock_producto` | `GET /api/productos/{id}/stock` |
| `consultar_bodegas_criticas` | `GET /api/bodegas/criticas` |
| `consultar_productos_en_riesgo` | `GET /api/productos/riesgo` |
| `consultar_kpis` | `GET /api/kpis` |
| `crear_orden_borrador` | `POST /api/ordenes` |
| `publicar_resumen` | `POST /api/panel/resumen` |

No existe herramienta para aprobar, cancelar ni recibir órdenes.

## Ejecución

```bash
cd mcp-server
cp .env.example .env   # opcional; también puede exportar variables
npm install
npm start
```

Por defecto escucha `http://localhost:3100/mcp` (transporte Streamable HTTP para n8n).

```bash
npm run start:stdio
```

Variables: `LOGITRACK_API_BASE_URL`, `LOGITRACK_USERNAME`, `LOGITRACK_PASSWORD`, `MCP_PORT`.
