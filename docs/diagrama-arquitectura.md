# Diagrama de arquitectura — LogiTrack IQ

Flujo de datos exigido por el reto integrador IA2.

```mermaid
flowchart LR
  subgraph Automatizacion
    N8N["n8n\nResumen diario de inventario\n06:00 America/Bogota"]
    MCP["MCP Server\n(rol AGENTE)"]
  end

  subgraph Backend
    API["Spring Boot API\nJWT + reglas de negocio"]
    DB[(PostgreSQL / H2\nschema logitrack)]
  end

  subgraph Presentacion
    DASH["Dashboard web\nstatic/ HTML+JS"]
  end

  N8N -->|"AI Agent + tools"| MCP
  MCP -->|"REST + Bearer JWT"| API
  DASH -->|"REST + Bearer JWT"| API
  API --> DB
```

## Responsabilidades

| Componente | Rol |
| --- | --- |
| **n8n** | Orquesta el agente diario: consulta KPIs/riesgo, crea máx. 1 orden BORRADOR, publica resumen. |
| **MCP** | Pasarela HTTP autenticada; 6 tools; sin reglas de negocio ni acceso directo a MySQL. |
| **API Spring Boot** | Fuente de verdad: stock, riesgo, órdenes, PDF, panel, auditoría. |
| **Dashboard** | Consume la API; ADMIN aprueba/recibe; visualiza PDF BORRADOR con watermark. |

## Tools MCP → API

| Tool | Endpoint |
| --- | --- |
| `consultar_stock_producto` | `GET /api/productos/{id}/stock` |
| `consultar_bodegas_criticas` | `GET /api/bodegas/criticas` |
| `consultar_productos_en_riesgo` | `GET /api/productos/riesgo` |
| `consultar_kpis` | `GET /api/kpis` |
| `crear_orden_borrador` | `POST /api/ordenes` |
| `publicar_resumen` | `POST /api/panel/resumen` |

No existe tool para aprobar, cancelar ni recibir órdenes.
