# LogiTrack

Sistema de gestión de bodegas, inventario, movimientos y auditoría para LogiTrack S.A. Incluye API REST con Spring Boot, persistencia JPA/Hibernate sobre PostgreSQL y una interfaz web estática servida por el mismo backend.

## Stack

- Java 17
- Spring Boot 4.1 (Web, Data JPA, Security, Validation)
- PostgreSQL (esquema `logitrack`; en este proyecto se usa Supabase)
- JWT (JJWT) para autenticación
- OpenAPI / Swagger (`springdoc`)
- Frontend SPA en `src/main/resources/static/` (HTML, CSS, JavaScript)

## Módulos funcionales

| Módulo | Descripción |
| --- | --- |
| Autenticación | Login JWT (`POST /auth/login`) |
| Usuarios | CRUD con roles `ADMIN`, `EMPLEADO` y `AGENTE` |
| Bodegas | CRUD de almacenes y encargado |
| Productos | CRUD; el stock **no** se guarda en `producto`, se calcula desde `inventario_bodega` |
| Stock inicial | Al crear un producto con stock &gt; 0 se exige una bodega y se inserta en `inventario_bodega` |
| Movimientos | Entrada, salida y transferencia; actualizan inventario en la misma transacción |
| Auditoría | INSERT / UPDATE / DELETE con usuario, entidad, id de registro, valor anterior y valor nuevo |
| Reportes | Inventario por bodega, movimientos filtrados y auditoría filtrada |

## Requisitos

- JDK 17+
- Maven Wrapper (`./mvnw`) o Maven 3.9+
- PostgreSQL con el esquema `logitrack` creado

## Configuración

La aplicación lee `logitrack/src/main/resources/application.properties`.

Variables de entorno recomendadas (no subas contraseñas al repositorio):

```bash
export DB_URL="jdbc:postgresql://HOST:PUERTO/postgres?sslmode=require&currentSchema=logitrack&prepareThreshold=0"
export DB_USERNAME="tu_usuario"
export DB_PASSWORD="tu_password"
```

Notas importantes:

- `prepareThreshold=0` evita errores de prepared statements con el **pooler transaccional** de Supabase (puerto 6543).
- `spring.jpa.hibernate.ddl-auto=none`: el esquema se gestiona con SQL, no con Hibernate.
- `spring.sql.init.mode=never`: no se reejecutan `schema.sql` / `data.sql` al arrancar.

Si la tabla `auditoria` ya existía sin la columna de trazabilidad, ejecuta en PostgreSQL:

```sql
ALTER TABLE logitrack.auditoria ADD COLUMN IF NOT EXISTS entidad_id BIGINT;
```

## Base de datos

Scripts en `logitrack/database/`:

1. **`schema.sql`** — crea el esquema `logitrack` y todas las tablas (reto anterior + IQ).
2. **`data.sql`** — datos de demostración IQ (usuarios, proveedores, productos en riesgo, órdenes BORRADOR).
3. **`schema_supabase.sql`** — script idempotente para Supabase o bases ya existentes (migración + datos IQ).

Ejecuta `schema.sql` y luego `data.sql` en una base vacía (psql, DBeaver o SQL Editor de Supabase).

Modelo resumido:

- `usuario` (roles `ADMIN`, `EMPLEADO`, `AGENTE`)
- `proveedor`, `producto` (con `proveedor_principal_id` opcional)
- `bodega` + `inventario_bodega` (stock por bodega)
- `movimiento` + `detalle_movimiento`
- `orden_compra`, `resumen_panel` (LogiTrack IQ)
- `auditoria`

## Cómo ejecutar

Desde `logitrack/`:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

- UI: [http://localhost:8080](http://localhost:8080)
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Swagger / OpenAPI

1. Abre Swagger UI y ejecuta `POST /auth/login` con `admin_logitrack` / `123456`.
2. Copia el `token` de la respuesta.
3. Pulsa **Authorize** → pega `Bearer <token>` (o solo el token, según la UI).
4. Prueba los endpoints IQ agrupados por tags: **IQ - KPIs**, **IQ - Órdenes**, **IQ - Panel**, etc.

Los esquemas documentan campos del PDF (`KpisResponse`, `ProductoRiesgoResponse`, contrato del panel). Solo se listan rutas `/api/**` para evitar duplicados con alias sin prefijo.

## Usuarios de prueba (si cargaste `data.sql`)

| Usuario | Rol | Contraseña de ejemplo |
| --- | --- | --- |
| `admin_logitrack` | ADMIN | `123456` (según comentario del script; el valor persistido está en BCrypt) |
| `empleado_1` | EMPLEADO | `123456` (ídem) |
| `agente_mcp` | AGENTE | `123456` (MCP y flujo n8n) |

Tras el login, el frontend guarda el JWT en `sessionStorage` y lo envía en `Authorization: Bearer ...`.

## LogiTrack IQ — Torre de control

Extensión del reto IA2: KPIs, productos en riesgo, órdenes de compra, panel operativo, PDF y rol `AGENTE`.

Documentación: [`../docs/README.md`](../docs/README.md) · SDD: [`../docs/sdd/`](../docs/sdd/) · Alineación PDF: [`../docs/alineacion-requisitos-pdf.md`](../docs/alineacion-requisitos-pdf.md) · Código: [`../docs/arquitectura-codigo.md`](../docs/arquitectura-codigo.md) · UI: [`../docs/frontend-dashboard.md`](../docs/frontend-dashboard.md).

### Endpoints IQ (JWT requerido)

| Método | Ruta | Rol |
| --- | --- | --- |
| GET | `/kpis`, `/api/kpis` | ADMIN, AGENTE |
| GET | `/productos/riesgo`, `/api/productos/riesgo` | ADMIN, AGENTE |
| GET | `/productos/{id}/stock`, `/api/productos/{id}/stock` | ADMIN, AGENTE |
| GET | `/bodegas/criticas`, `/api/bodegas/criticas` | ADMIN, AGENTE |
| GET | `/proveedores`, `/api/proveedores` | ADMIN, AGENTE |
| GET/POST | `/ordenes`, `/api/ordenes` | ADMIN, AGENTE (POST crea `BORRADOR`) |
| PATCH | `/ordenes/{id}/estado`, `/api/ordenes/{id}/estado` | **Solo ADMIN** |
| POST/GET | `/ordenes/{id}/pdf` | ADMIN, AGENTE |
| POST/GET | `/panel/resumen`, `/api/panel/resumen` | ADMIN, AGENTE |
| POST | `/api/movimientos` | **Solo ADMIN** |

### MCP y n8n

- Servidor MCP: [`../mcp-server/`](../mcp-server/)
- Skill: [`../skills/operacion-logitrack/SKILL.md`](../skills/operacion-logitrack/SKILL.md)
- Flujo n8n: [`../n8n/resumen-diario-inventario.json`](../n8n/resumen-diario-inventario.json) (cron 06:00 `America/Bogota`)

Casos de prueba IQ: `src/test/java/com/example/logitrack/iq/`.

## API principal

Prefijo `/api`. Casi todos los recursos requieren JWT; `/api/auth/**` es público.

- `POST /auth/login`
- `GET|POST|PUT|DELETE /api/bodegas`
- `GET|POST|PUT|DELETE /api/productos` — el POST acepta `stock` y `bodegaId` para inventario inicial
- `GET /api/productos/stock-bajo`
- `GET|POST /api/movimientos`
- `GET|POST|PUT|DELETE /api/usuarios`
- `GET /api/auditoria` — respuesta en DTO (`usuario`, `entidadId`, valores anterior/nuevo)
- `GET /api/reportes/inventario?bodegaId=`
- `GET /api/reportes/movimientos?bodega=&producto=&tipoMovimiento=&fechaInicio=&fechaFin=`
- `GET /api/reportes/auditoria?entidadAfectada=&fechaInicio=&fechaFin=`

Fechas de reportes en ISO-8601 (`2026-08-24T00:00:00`).

## Decisiones de diseño

1. **Stock por bodega.** El campo `stock` de `Producto` es `@Transient`: se suma `inventario_bodega`. Crear un producto con cantidad inicial escribe esa tabla; los cambios posteriores van por movimientos.
2. **Transacciones.** Crear movimiento, actualizar inventario y registrar auditoría ocurren en la misma `@Transactional`. Si falla el stock (por ejemplo, salida sin existencias), se hace rollback de todo.
3. **Auditoría.** Se persiste el usuario responsable (JOIN FETCH / EAGER), el id de la entidad y un resumen textual de valores. La API no serializa el proxy Hibernate ni la contraseña.
4. **JWT stateless.** Sin sesiones de servidor; el frontend reenvía el token en cada petición.

## Estructura del código

```
logitrack/
  database/                 # schema.sql y data.sql
  src/main/java/com/example/logitrack/
    controller/
    dto/
    model/
    repository/
    security/
    service/
    exception/
  src/main/resources/
    application.properties
    static/                 # index.html, app.js, style.css
```

## Pruebas

```bash
./mvnw test
```

## Licencia

Proyecto académico de LogiTrack S.A.
