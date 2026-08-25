# Servidor MCP — LogiTrack IQ

Pasarela MCP (Python + FastMCP) hacia la API REST de Spring Boot. Autentica al usuario `agente_mcp` (rol `AGENTE`), inyecta el JWT en cada petición y **no** implementa reglas de negocio ni herramientas para aprobar órdenes.

## Herramientas (exactamente 6)

| Tool | API |
| --- | --- |
| `consultar_stock_producto` | `GET /api/productos/{id}/stock` |
| `consultar_bodegas_criticas` | `GET /api/bodegas/criticas` |
| `consultar_productos_en_riesgo` | `GET /api/productos/riesgo` |
| `consultar_kpis` | `GET /api/kpis` |
| `crear_orden_borrador` | `POST /api/ordenes` |
| `publicar_resumen` | `POST /api/panel/resumen` |

## Requisitos

- Python 3.10+
- Backend Spring Boot en `http://localhost:8080`
- Usuario `agente_mcp` / `123456`

## Instalación

```bash
cd mcp-server
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

Copia las variables de entorno:

```bash
cp .env.example .env
```

## Variables de entorno

| Variable | Default | Descripción |
| --- | --- | --- |
| `LOGITRACK_API_BASE_URL` | `http://localhost:8080` | URL del backend Spring Boot |
| `LOGITRACK_USERNAME` | `agente_mcp` | Usuario con rol `AGENTE` |
| `LOGITRACK_PASSWORD` | `123456` | Contraseña del agente |
| `MCP_TRANSPORT` | `stdio` | `stdio`, `streamable-http` o `sse` |
| `MCP_HOST` | `127.0.0.1` | Host de escucha (HTTP/SSE) |
| `MCP_PORT` | `8000` | Puerto de escucha (HTTP/SSE) |
| `MCP_STATELESS_HTTP` | `true` | Sin sesión MCP persistente (recomendado para n8n) |
| `MCP_JSON_RESPONSE` | `true` | Respuestas JSON en POST (evita handshake SSE obligatorio) |

## Ejecución — stdio (Cursor / Claude Desktop)

Modo por defecto. No abre puerto HTTP; el cliente MCP lanza el proceso:

```bash
python main.py
```

Ejemplo de config MCP en Cursor:

```json
{
  "mcpServers": {
    "logitrack-iq": {
      "command": "python",
      "args": ["/ruta/absoluta/a/mcp-server/main.py"],
      "env": {
        "LOGITRACK_API_BASE_URL": "http://localhost:8080",
        "LOGITRACK_USERNAME": "agente_mcp",
        "LOGITRACK_PASSWORD": "123456",
        "MCP_TRANSPORT": "stdio"
      }
    }
  }
}
```

## Ejecución — HTTP Streamable (n8n)

Para que n8n se conecte por red, usa **Streamable HTTP** servido con **Uvicorn** en el puerto **3100** (coincide con `n8n/resumen-diario-inventario.json`):

```bash
export MCP_TRANSPORT=streamable-http
export MCP_HOST=0.0.0.0
export MCP_PORT=3100

python main.py
```

El servidor usa modo **stateless** + respuestas **JSON** por defecto, compatible con el nodo n8n `httpStreamable` sin exigir handshake SSE con `mcp-session-id`.

Endpoint MCP:

```text
http://localhost:3100/mcp
```

Verificación del handshake MCP (`initialize`):

```bash
curl -s -X POST http://localhost:3100/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}'
```

### Configuración del nodo MCP en n8n

| Campo | Valor |
| --- | --- |
| **Endpoint URL** | `http://host.docker.internal:3100/mcp` (n8n en Docker, Mac/Windows) |
| | `http://localhost:3100/mcp` (n8n nativo en el mismo host) |
| **Server transport** | `httpStreamable` |
| **Authentication** | `none` |

> Usa `0.0.0.0` como host para que n8n en Docker pueda alcanzar el servidor desde `host.docker.internal`.

## Ejecución — SSE (legacy)

Solo si el cliente exige SSE en lugar de Streamable HTTP:

```bash
export MCP_TRANSPORT=sse
export MCP_HOST=0.0.0.0
export MCP_PORT=3100

python main.py
```

Endpoint: `http://localhost:3100/sse`

## Notas

- El backend Spring Boot debe estar levantado **antes** de iniciar el MCP.
- No ejecutes stdio y HTTP al mismo tiempo en el mismo terminal/proceso.
- `streamable-http` es el transporte recomendado para n8n; `sse` queda solo por compatibilidad.
