# Arquitectura en código — LogiTrack IQ

Mapa del código fuente con responsabilidades y enlaces al PDF del reto. Complementa [`diagrama-arquitectura.md`](diagrama-arquitectura.md).

---

## Capas del backend (`logitrack/`)

```
HTTP Request
    ↓
JwtAuthenticationFilter          security/JwtAuthenticationFilter.java
    ↓
SecurityConfig (roles)           security/SecurityConfig.java
    ↓
Controller (REST)                controller/*
    ↓
Service (reglas de negocio)      service/*
    ↓
Repository (JPA)                 repository/*
    ↓
PostgreSQL (schema logitrack)    database/schema.sql
```

**Principio:** Toda regla de inventario, riesgo, órdenes y panel vive en **services**, no en controladores, MCP ni frontend.

---

## Controladores IQ

| Controlador | Rutas | Responsabilidad |
| --- | --- | --- |
| `KpiController` | `GET /kpis`, `/api/kpis` | Delega a `IndicadoresInventarioService.obtenerKpis()` |
| `ProductoController` | `GET .../riesgo`, `.../{id}/stock` | Riesgo y stock calculado |
| `BodegaController` | `GET .../criticas` | Ocupación ≥ 90% |
| `ProveedorController` | `GET /proveedores` | Listado precargado |
| `OrdenCompraController` | CRUD órdenes, PDF, PATCH estado | Orquesta `OrdenCompraService` + `OrdenPdfService` |
| `PanelResumenController` | `POST/GET /panel/resumen` | `PanelResumenService` |

Ubicación: `logitrack/src/main/java/com/example/logitrack/controller/`.

---

## Servicios IQ (núcleo de negocio)

### `IndicadoresInventarioService`

**Archivo:** `service/IndicadoresInventarioService.java`

| Método | Regla PDF | Por qué aquí |
| --- | --- | --- |
| `obtenerKpis()` | 4 indicadores + ayer + calculadoEn | Un solo punto para dashboard y MCP |
| `consumoDiarioPromedio()` | Salidas 30 días / 30 | Base del punto de reorden |
| `puntoReorden()` | consumo × diasEntrega × 1.5 | Centraliza fórmula |
| `calcularCobertura()` | null + SIN_CONSUMO si consumo 0 | Evita división por cero en UI |
| `estaEnRiesgo()` | stock < punto, con proveedor | Criterio único para listados |
| `listarProductosEnRiesgo()` | Campos DTO del GET riesgo | Incluye bodegaDestinoId sugerida |
| `listarBodegasCriticas()` | ocupación ≥ 90% | |
| `sugerirBodegaDestinoId()` | menor stock, empate → menor id | |
| `obtenerStockProducto()` | Stock total + desglose | |

Constante `ZONA_BOGOTA = America/Bogota` usada en “ayer” y fechas de panel.

### `OrdenCompraService`

**Archivo:** `service/OrdenCompraService.java`

| Método | Regla PDF |
| --- | --- |
| `crear()` | Estado BORRADOR, total en servidor, cantidad > 0 |
| `cambiarEstado()` | Transiciones válidas; 403 si no admin; invalida PDF |
| `registrarEntradaRecepcion()` | Movimiento ENTRADA al pasar a RECIBIDA |
| `transicionPermitida()` | Tabla de estados del PDF |

Anotación `@Transactional` en recepción: orden + movimiento atómicos.

### `OrdenPdfService`

**Archivo:** `service/OrdenPdfService.java`

| Método | Regla PDF |
| --- | --- |
| `generar()` | Campos obligatorios en PDF |
| `dibujarMarcaAguaDiagonal()` | Texto BORRADOR si estado BORRADOR |

Usa OpenPDF (`com.lowagie.text`).

### `PanelResumenService`

**Archivo:** `service/PanelResumenService.java`

| Método | Regla PDF |
| --- | --- |
| `publicar()` | Upsert por fecha, auditoría |
| `validarContrato()` | Fecha hoy, narrativa, enums |
| `validarExistenciaIds()` | IDs en BD |

---

## Modelos JPA IQ

| Clase | Tabla | Campos clave |
| --- | --- | --- |
| `Proveedor` | `proveedores` | `diasEntrega` 1–90 |
| `Producto` | `productos` | `proveedorPrincipal` (ManyToOne opcional) |
| `OrdenCompra` | `orden_compra` | `estado`, `pdf` (byte[]), relaciones producto/proveedor/bodega |
| `ResumenPanel` | `resumen_panel` | `fecha` unique, `contenidoJson` |
| `EstadoOrdenCompra` | enum | BORRADOR, APROBADA, RECIBIDA, CANCELADA |
| `Rol` | enum | ADMIN, EMPLEADO, **AGENTE** |

`logitrack/src/main/java/com/example/logitrack/model/`.

---

## Seguridad

**Archivo:** `security/SecurityConfig.java`

| Patrón | Roles |
| --- | --- |
| `PATCH /ordenes/**` | ADMIN |
| `POST /api/movimientos` | ADMIN |
| `/kpis`, `/productos/riesgo`, `/ordenes` (GET/POST), `/panel/**` | ADMIN, AGENTE |
| `/auth/login` | público |

**Archivo:** `security/JwtAuthenticationFilter.java` — extrae Bearer y valida token.

---

## Excepciones

**Archivo:** `exception/GlobalExceptionHandler.java`

Mapea validaciones a **400**, `AccessDeniedException` a **403**, `EntityNotFound` a **404**, auth a **401** (reutilizado del reto anterior).

---

## Pruebas

| Archivo | Tipo | Qué certifica |
| --- | --- | --- |
| `test/.../IndicadoresInventarioServiceTest.java` | Unitario | Cobertura, riesgo, ocupación |
| `test/.../OrdenPdfServiceTest.java` | Unitario | Watermark PDF |
| `test/.../iq/OrdenCompraEstadoTest.java` | Integración `@SpringBootTest` | Estados, PDF, AGENTE 403, ENTRADA |
| `test/.../iq/PanelResumenTest.java` | Integración | Contrato resumen |

Paquete `iq/` usa `@ActiveProfiles("test")` y H2 en memoria para integración.

---

## MCP (`mcp-server/`)

| Archivo | Rol |
| --- | --- |
| `main.py` | 6 tools FastMCP, login JWT, HTTP a API |
| `schemas.py` | Validación estructura alertas/acciones (espejo del contrato) |
| `.env.example` | Variables `LOGITRACK_*`, `MCP_*` |

Sin imports de base de datos; sin lógica de punto de reorden.

---

## n8n (`n8n/`)

| Archivo | Contenido |
| --- | --- |
| `resumen-diario-inventario.json` | Workflow exportado: Cron, AI Agent, Gemini, nodo MCP, IF éxito/error |

Skill embebida en el prompt del AI Agent (copia de `skills/operacion-logitrack/SKILL.md`).

---

## Frontend (`static/`)

| Archivo | Sección IQ |
| --- | --- |
| `app.js` | `loadDashboard`, `renderKpis`, `cambiarEstadoOrden`, PDF |
| `index.html` | `#page-dashboard`, tablas riesgo/BORRADOR |
| `style.css` | KPI, ocupación, responsive, alertas panel |

---

## Base de datos (`logitrack/database/`)

| Script | Uso |
| --- | --- |
| `schema.sql` | DDL completo PostgreSQL |
| `data.sql` | Usuarios, proveedores, productos en riesgo, movimientos ENTRADA iniciales |
| `schema_supabase.sql` | Migración idempotente para Supabase |

---

## Flujo de datos completo (referencia)

```
n8n (06:00)
  → MCP.consultar_kpis / consultar_productos_en_riesgo
  → [opcional] crear_orden_borrador (1 vez)
  → publicar_resumen
  → PostgreSQL

ADMIN (dashboard)
  → PATCH APROBADA → PATCH RECIBIDA
  → OrdenCompraService crea ENTRADA
  → inventario_bodega actualizado
  → loadDashboard() refleja nuevos KPIs
```

---

## Enlaces

- [Alineación requisitos PDF](alineacion-requisitos-pdf.md)
- [Frontend y responsive](frontend-dashboard.md)
- [Evidencia SDD/TDD](sdd/evidencia-sdd.md)
