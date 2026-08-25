# Proyecto LogiTrack AI

Aplicación **LogiTrack**: gestión de bodegas, inventario, movimientos, auditoría, reportes y **torre de control IQ** (KPIs, órdenes de compra, panel n8n/MCP).

Documentación SDD: [`docs/sdd/`](docs/sdd/). Diagrama: [`docs/diagrama-arquitectura.md`](docs/diagrama-arquitectura.md).

## Estructura del repositorio

```
docs/sdd/          Documentación SDD + evidencia TDD
logitrack/         Backend Spring Boot + dashboard (static/)
mcp-server/        Servidor MCP (rol AGENTE)
n8n/               Flujo Resumen diario de inventario
skills/            Skill operacion-logitrack
```

## Requisitos

- JDK 17+
- PostgreSQL (esquema `logitrack`)
- Python 3.11+ (servidor MCP)
- n8n con nodo AI Agent (flujo diario)

## Base de datos

Ejecuta en PostgreSQL (base vacía), en este orden:

1. [`logitrack/database/schema.sql`](logitrack/database/schema.sql) — tablas del reto anterior + IQ
2. [`logitrack/database/data.sql`](logitrack/database/data.sql) — usuarios, proveedores, productos en riesgo, movimientos y órdenes semilla

Si ya tienes una base del reto anterior o usas Supabase, puedes usar el script idempotente [`logitrack/database/schema_supabase.sql`](logitrack/database/schema_supabase.sql) (migra y carga datos IQ).

## Usuarios de prueba

Contraseña para todos: **`123456`**

| Usuario | Rol | Uso |
| --- | --- | --- |
| `admin_logitrack` | ADMIN | Dashboard: aprobar, recibir, movimientos |
| `agente_mcp` | AGENTE | MCP, n8n, crear órdenes BORRADOR |
| `empleado_1` | EMPLEADO | Reto anterior (CRUD básico) |

## Inicio rápido

### 1. Backend

```bash
cd logitrack
export DB_URL="jdbc:postgresql://HOST:5432/postgres?currentSchema=logitrack"
export DB_USERNAME="tu_usuario"
export DB_PASSWORD="tu_password"
./mvnw spring-boot:run
```

- UI / dashboard: [http://localhost:8080](http://localhost:8080)
- Swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Login: `POST /auth/login` con `{ "username", "password" }`

### 2. Servidor MCP

```bash
cd mcp-server
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
MCP_TRANSPORT=streamable-http MCP_PORT=3100 python main.py
```

Usa `agente_mcp` / `123456` contra la API en `http://localhost:8080`.

### 3. Flujo n8n

Importa [`n8n/resumen-diario-inventario.json`](n8n/resumen-diario-inventario.json). El cron está configurado a las **06:00 America/Bogota**. Asegúrate de que el MCP esté en marcha y la API accesible.

Skill del agente: [`skills/operacion-logitrack/SKILL.md`](skills/operacion-logitrack/SKILL.md).

## Flujo IQ de demostración

1. Datos iniciales con productos en riesgo (`data.sql`).
2. Ejecutar manualmente el flujo n8n → crea como máximo una orden `BORRADOR`.
3. Login como `admin_logitrack` → aprobar la orden en el dashboard.
4. Recibir la orden (botón **Recibir** en el dashboard o página Órdenes).
5. Verificar KPIs actualizados y movimiento `ENTRADA`.

## Pruebas

```bash
cd logitrack && ./mvnw test
```

Detalle de endpoints y arquitectura: [`logitrack/README.md`](logitrack/README.md).

Evidencia operativa (MCP, n8n, PDF): [`docs/evidencia/README.md`](docs/evidencia/README.md).
