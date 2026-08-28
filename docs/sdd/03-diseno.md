# 03 — Diseño: entidades, decisiones y flujo

Extensión del backend LogiTrack existente. Las responsabilidades nuevas se separan de forma clara (API, MCP, n8n, frontend, skills, docs) sin reemplazar el modelo de bodegas, productos, movimientos, usuarios ni auditoría ya construido.

## Decisiones de diseño (del reto)

- Única fuente de verdad: API Spring Boot + base de datos.
- MCP es un adaptador HTTP autenticado como `AGENTE`; no hay reglas de negocio en MCP ni en n8n sobre MySQL.
- Cálculos de stock, riesgo, KPIs y totales de orden se ejecutan en el backend.
- Recepción de orden y movimiento `ENTRADA` en **una transacción**.
- PDF persistido en la orden; se invalida al cambiar de estado.
- Un resumen válido por `fecha`; reemplazo + auditoría.
- Zona horaria `America/Bogota` para “hoy”, “ayer” y el cron de n8n.

## Entidades nuevas o adaptadas

### Proveedor

| Campo | Notas |
| --- | --- |
| `id` | Identificador |
| `nombre` | |
| `contacto` | |
| `diasEntrega` | Entero entre 1 y 90 |

Carga reproducible (`data.sql` o equivalente).

### Producto (adaptación)

Relación **opcional** `proveedorPrincipal` (`ManyToOne` → `Proveedor`). Sin proveedor principal no hay riesgo ni orden automática.

El stock de negocio se deriva de movimientos, no de `Producto.stock` legado.

### OrdenCompra

| Campo | Notas |
| --- | --- |
| `id` | |
| `producto` | Exactamente uno |
| `proveedor` | |
| `bodegaDestino` | Obligatoria |
| `cantidad` | Mayor que 0 |
| `precioUnitario` | |
| `total` | Calculado en servidor al crear |
| `fechaCreacion` | |
| `estado` | `BORRADOR` \| `APROBADA` \| `RECIBIDA` \| `CANCELADA` |
| `creadoPor` | Usuario |
| PDF (opcional) | Documento generado y fecha de generación |

### ResumenPanel

| Campo | Notas |
| --- | --- |
| `id` | |
| `fecha` | Un resumen válido por fecha |
| `contenidoJson` | Cuerpo del contrato del panel |
| `autor` | Usuario que publica |

### Rol `AGENTE`

Nuevo valor de rol, junto a `ADMIN` (y el rol `EMPLEADO` existente del proyecto anterior, que no sustituye las reglas de la tabla AGENTE/ADMIN del reto IQ).

Permisos IQ: consultar KPIs/stock/riesgos/críticas; crear `BORRADOR`; publicar resumen. No aprobar/recibir/cancelar ni movimientos manuales.

## Relaciones

```
Proveedor 1 ──< Producto (proveedorPrincipal, opcional)
Proveedor 1 ──< OrdenCompra
Producto  1 ──< OrdenCompra
Bodega    1 ──< OrdenCompra (bodegaDestino)
Usuario   1 ──< OrdenCompra (creadoPor)
Usuario   1 ──< ResumenPanel (autor)

OrdenCompra (RECIBIDA) ──crea──> Movimiento ENTRADA
                                 └── DetalleMovimiento (producto, cantidad)
                                 └── bodegaDestino
```

Auditoría existente registra: creación de orden, publicación/reemplazo de resumen, transición y recepción.

## Diagrama del flujo n8n → MCP → Spring Boot → Dashboard

```text
                    06:00 America/Bogota
                    Schedule Trigger (n8n)
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Flujo n8n           │
                 │ "Resumen diario     │
                 │  de inventario"     │
                 │ AI Agent + skill    │
                 └──────────┬──────────┘
                            │ herramientas MCP
                            │ (JWT usuario AGENTE)
                            ▼
                 ┌─────────────────────┐
                 │ Servidor MCP        │
                 │ 6 tools, sin        │
                 │ aprobar órdenes     │
                 └──────────┬──────────┘
                            │ HTTP REST
                            ▼
                 ┌─────────────────────┐
                 │ API Spring Boot     │
                 │ /kpis /riesgo       │
                 │ POST /ordenes       │
                 │ POST /panel/resumen │
                 └──────────┬──────────┘
                            │ JPA
                            ▼
                 ┌─────────────────────┐
                 │ Base de datos       │
                 │ (única fuente)      │
                 └──────────┬──────────┘
                            │ GET reales
                            ▼
                 ┌─────────────────────┐
                 │ Dashboard           │
                 │ frontend/           │
                 │ JWT sessionStorage  │
                 │ ADMIN: Aprobar      │
                 └─────────────────────┘
```

Flujo de negocio posterior (humano, no MCP):

```text
ADMIN  --PATCH estado APROBADA-->  Orden
ADMIN  --PATCH estado RECIBIDA-->  Orden + Movimiento ENTRADA (misma TX)
Dashboard refresca indicadores e inventario
```

## Cantidad de la orden automática (n8n)

Para el **primer** producto de `GET /productos/riesgo`:

`cantidad = ceil(max(1, puntoReorden × 2 - stockTotal))`

Máximo **una** orden `BORRADOR` por ejecución.

## Estructura de referencia del entregable

```
src/                          # backend anterior extendido
frontend/
mcp-server/
n8n/resumen-diario-inventario.json
skills/operacion-logitrack/SKILL.md
docs/sdd/
README.md
schema.sql / data.sql (o equivalente reproducible)
```

La estructura exacta puede adaptarse al proyecto anterior (`logitrack/` + `static/`) siempre que las responsabilidades estén separadas de forma clara. El reto pide dashboard en `frontend/`; en este repositorio el código UI está en `logitrack/src/main/resources/static/` y la carpeta raíz `frontend/` documenta esa decisión ([`../../frontend/README.md`](../../frontend/README.md)).

## Wireframes y UI

Bocetos de pantallas IQ (login, dashboard, órdenes, flujos n8n y responsive):

- [`../wireframes.md`](../wireframes.md)
- [`../frontend-dashboard.md`](../frontend-dashboard.md) — breakpoints 1024 / 768 / 480 px y checklist de verificación

## Mapa de código

Servicios, controladores y pruebas por capa: [`../arquitectura-codigo.md`](../arquitectura-codigo.md).
