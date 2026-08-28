# Diagrama de arquitectura — LogiTrack IQ

Flujo de datos exigido por el reto integrador IA2. La base de datos implementada es **PostgreSQL** (esquema `logitrack`); el PDF usa “MySQL” como metáfora de “no acceder a la BD desde MCP/n8n/dashboard”.

## Vista general

```mermaid
flowchart TB
  subgraph Automatizacion["Automatización (rol AGENTE)"]
    N8N["n8n\nResumen diario de inventario\nCron 06:00 America/Bogota"]
    SKILL["skills/operacion-logitrack\nSKILL.md"]
    MCP["mcp-server\n6 tools HTTP"]
  end

  subgraph Backend["Backend — fuente de verdad"]
    API["Spring Boot API\nJWT + reglas de negocio"]
    DB[(PostgreSQL\nschema logitrack)]
  end

  subgraph Presentacion["Presentación"]
    DASH["Dashboard web\nstatic/ HTML+CSS+JS\nsessionStorage JWT"]
  end

  N8N --> SKILL
  N8N -->|"AI Agent + MCP Client"| MCP
  MCP -->|"REST Bearer JWT\nsin acceso directo a BD"| API
  DASH -->|"REST Bearer JWT\nsolo lectura/acciones ADMIN"| API
  API --> DB
```

## Flujo de negocio de punta a punta

```mermaid
sequenceDiagram
  autonumber
  participant Cron as n8n Schedule
  participant Agent as AI Agent
  participant MCP as MCP Server
  participant API as Spring Boot
  participant DB as PostgreSQL
  participant Admin as Dashboard ADMIN

  Cron->>Agent: 06:00 Bogotá
  Agent->>MCP: consultar_kpis / productos_en_riesgo
  MCP->>API: GET /api/kpis, /api/productos/riesgo
  API->>DB: cálculos stock/riesgo
  opt Máximo 1 producto en riesgo
    Agent->>MCP: crear_orden_borrador
    MCP->>API: POST /api/ordenes (BORRADOR)
    API->>DB: INSERT orden_compra
  end
  Agent->>MCP: publicar_resumen
  MCP->>API: POST /api/panel/resumen
  API->>DB: UPSERT resumen_panel

  Admin->>API: PATCH orden APROBADA (solo ADMIN)
  Admin->>API: PATCH orden RECIBIDA
  API->>DB: UPDATE orden + INSERT movimiento ENTRADA
  Admin->>API: GET /api/kpis
  API-->>Admin: inventario actualizado
```

## Responsabilidades por componente

| Componente | Rol | Qué **no** hace |
| --- | --- | --- |
| **n8n** | Orquesta el agente diario: KPIs, riesgo, ≤1 BORRADOR, resumen | No aprueba órdenes; no escribe en BD |
| **MCP** | Pasarela HTTP con JWT `AGENTE`; 6 tools fijas | Sin reglas de negocio; sin SQL |
| **API Spring Boot** | Stock, riesgo, KPIs, órdenes, PDF, panel, auditoría | Única fuente de verdad |
| **PostgreSQL** | Persistencia | Solo vía JPA desde API |
| **Dashboard** | Visualiza datos; ADMIN cambia estados | No calcula inventario |

## Tools MCP → API

| Tool | Endpoint |
| --- | --- |
| `consultar_stock_producto` | `GET /api/productos/{id}/stock` |
| `consultar_bodegas_criticas` | `GET /api/bodegas/criticas` |
| `consultar_productos_en_riesgo` | `GET /api/productos/riesgo` |
| `consultar_kpis` | `GET /api/kpis` |
| `crear_orden_borrador` | `POST /api/ordenes` |
| `publicar_resumen` | `POST /api/panel/resumen` |

**No existe** tool para aprobar, cancelar ni recibir órdenes (restricción obligatoria del PDF).

## Ubicación en el repositorio

| Pieza | Ruta |
| --- | --- |
| API + servicios IQ | `logitrack/src/main/java/com/example/logitrack/` |
| Dashboard | `logitrack/src/main/resources/static/` |
| MCP | `mcp-server/main.py` |
| Flujo n8n | `n8n/resumen-diario-inventario.json` |
| Skill | `skills/operacion-logitrack/SKILL.md` |
| Esquema BD | `logitrack/database/schema.sql` |

## Documentación relacionada

- [Arquitectura en código](arquitectura-codigo.md)
- [Alineación con el PDF](alineacion-requisitos-pdf.md)
- [Wireframes](wireframes.md)
