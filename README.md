# Proyecto LogiTrack AI — Torre de control IQ

Extensión del backend **LogiTrack** (Spring Boot) para el reto integrador **IA2 — LogiTrack IQ**: monitoreo de inventario, detección de productos en riesgo, órdenes de compra en borrador, panel operativo vía n8n/MCP y dashboard web para el administrador.

> Documentación completa: **[`docs/README.md`](docs/README.md)** · Alineación con el PDF: **[`docs/alineacion-requisitos-pdf.md`](docs/alineacion-requisitos-pdf.md)**

## Qué hace el sistema (flujo de negocio)

1. Calcula el stock real desde **movimientos** (no desde un campo fijo en producto).
2. Detecta productos **en riesgo** (stock &lt; punto de reorden, con proveedor principal).
3. Cada día a las **06:00 America/Bogota**, n8n consulta la API vía **MCP** y puede crear **como máximo una** orden `BORRADOR`.
4. Un **ADMIN** revisa el dashboard, aprueba y recibe la orden.
5. Al recibir, el backend registra un movimiento **ENTRADA** en la bodega destino (transacción única).
6. El dashboard muestra KPIs, alertas del resumen y inventario actualizado.

## Estructura del repositorio

```
docs/              Documentación SDD, wireframes, alineación PDF, evidencias
logitrack/         Backend Spring Boot + dashboard (src/main/resources/static/)
mcp-server/        Servidor MCP Python (6 tools, rol AGENTE)
n8n/               Flujo "Resumen diario de inventario"
skills/            Skill operacion-logitrack para el agente
frontend/          Índice documental → el código UI está en logitrack/.../static/
```

## Requisitos

| Componente | Versión |
| --- | --- |
| JDK | 17+ |
| PostgreSQL | Esquema `logitrack` (scripts en `logitrack/database/`) |
| Python | 3.10+ (servidor MCP) |
| n8n | Con nodo AI Agent y MCP Client |

## Base de datos

Ejecutar en PostgreSQL (base vacía), en orden:

1. [`logitrack/database/schema.sql`](logitrack/database/schema.sql) — tablas reto anterior + IQ
2. [`logitrack/database/data.sql`](logitrack/database/data.sql) — usuarios, proveedores, productos en riesgo, movimientos `ENTRADA` iniciales

Para Supabase o migración: [`logitrack/database/schema_supabase.sql`](logitrack/database/schema_supabase.sql).

## Usuarios de prueba

Contraseña para todos: **`123456`**

| Usuario | Rol | Uso en la demostración |
| --- | --- | --- |
| `admin_logitrack` | ADMIN | Aprobar, recibir órdenes; movimientos manuales |
| `agente_mcp` | AGENTE | MCP, n8n, crear órdenes BORRADOR, publicar resumen |
| `empleado_1` | EMPLEADO | Módulos del reto anterior (sin aprobar órdenes) |

## Inicio rápido

### 1. Backend y dashboard

```bash
cd logitrack
export DB_URL="jdbc:postgresql://HOST:5432/postgres?currentSchema=logitrack"
export DB_USERNAME="tu_usuario"
export DB_PASSWORD="tu_password"
./mvnw spring-boot:run
```

| Recurso | URL |
| --- | --- |
| Dashboard | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Login API | `POST /auth/login` `{ "username", "password" }` |

### 2. Servidor MCP

```bash
cd mcp-server
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
MCP_TRANSPORT=streamable-http MCP_PORT=3100 python main.py
```

Usa `agente_mcp` / `123456` contra `http://localhost:8080`.

### 3. Flujo n8n

Importar [`n8n/resumen-diario-inventario.json`](n8n/resumen-diario-inventario.json). Cron: **06:00 America/Bogota**. Requiere MCP en `:3100` y API accesible.

Skill del agente: [`skills/operacion-logitrack/SKILL.md`](skills/operacion-logitrack/SKILL.md).

## Demostración del flujo IQ

1. Cargar `data.sql` → productos en riesgo visibles en `GET /productos/riesgo`.
2. Ejecutar manualmente el flujo n8n → crea ≤1 orden `BORRADOR` + publica resumen.
3. Login como `admin_logitrack` → dashboard → **Aprobar** orden.
4. **Recibir** orden → verificar movimiento `ENTRADA` y KPIs actualizados.
5. Generar PDF en BORRADOR → comprobar marca de agua diagonal.

Evidencias: [`docs/evidencia/README.md`](docs/evidencia/README.md).

## Pruebas automatizadas

```bash
cd logitrack && ./mvnw test
```

Trazabilidad regla → prueba: [`docs/sdd/evidencia-sdd.md`](docs/sdd/evidencia-sdd.md).

## Documentación destacada

| Tema | Enlace |
| --- | --- |
| Índice general | [`docs/README.md`](docs/README.md) |
| Requisito PDF → código | [`docs/alineacion-requisitos-pdf.md`](docs/alineacion-requisitos-pdf.md) |
| Wireframes | [`docs/wireframes.md`](docs/wireframes.md) |
| Dashboard y responsive | [`docs/frontend-dashboard.md`](docs/frontend-dashboard.md) |
| Mapa de código | [`docs/arquitectura-codigo.md`](docs/arquitectura-codigo.md) |
| Diagrama n8n→MCP→API | [`docs/diagrama-arquitectura.md`](docs/diagrama-arquitectura.md) |
| SDD (propuesta → tareas) | [`docs/sdd/`](docs/sdd/) |
| Backend detallado | [`logitrack/README.md`](logitrack/README.md) |

## API IQ (resumen)

Prefijo opcional `/api`. JWT obligatorio salvo login.

| Método | Ruta | Rol |
| --- | --- | --- |
| GET | `/kpis` | ADMIN, AGENTE |
| GET | `/productos/riesgo`, `/productos/{id}/stock` | ADMIN, AGENTE |
| GET | `/bodegas/criticas`, `/proveedores` | ADMIN, AGENTE |
| GET/POST | `/ordenes` | ADMIN, AGENTE |
| PATCH | `/ordenes/{id}/estado` | **Solo ADMIN** |
| POST/GET | `/ordenes/{id}/pdf` | ADMIN, AGENTE |
| POST/GET | `/panel/resumen` | ADMIN, AGENTE |

Detalle y ejemplos JSON: [`docs/sdd/02-especificacion.md`](docs/sdd/02-especificacion.md).

## Licencia

Proyecto académico — LogiTrack S.A.
