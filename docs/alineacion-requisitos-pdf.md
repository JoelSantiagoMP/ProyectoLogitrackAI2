# Alineación con el PDF del reto — LogiTrack IQ

Matriz de trazabilidad entre el documento **Proyecto integrador IA2 — LogiTrack IQ** y la implementación actual. Para cada requisito se indica: **estado**, **justificación** y **ubicación en el código o evidencia**.

Leyenda: ✅ Cumple · ⚠️ Divergencia documentada (sin impacto funcional) · ❌ No cumple

---

## 1. Objetivo general y flujo de negocio

| # | Requisito PDF | Estado | Implementación y justificación |
| --- | --- | --- | --- |
| 1.1 | Extender backend existente, no reemplazarlo | ✅ | Se conservan bodegas, productos, movimientos, JWT, auditoría. Nuevas entidades IQ en el mismo módulo `logitrack/`. |
| 1.2 | Calcular inventario real desde movimientos | ✅ | `IndicadoresInventarioService` suma `inventario_bodega` y movimientos. `Producto.stock` es `@Transient`. Ver `logitrack/src/main/java/com/example/logitrack/service/IndicadoresInventarioService.java`. |
| 1.3 | Detectar productos bajo punto de reorden | ✅ | `listarProductosEnRiesgo()`, regla `stock < puntoReorden` con proveedor obligatorio. |
| 1.4 | n8n crea máx. 1 orden BORRADOR vía MCP | ✅ | Flujo `n8n/resumen-diario-inventario.json` + skill + `crear_orden_borrador`. Evidencia: `docs/evidencia/n8n-ejecucion.md`. |
| 1.5 | ADMIN aprueba en dashboard | ✅ | `app.js` → `cambiarEstadoOrden(id, 'APROBADA')` solo si `isAdmin()`. `SecurityConfig` restringe `PATCH /ordenes/**` a ADMIN. |
| 1.6 | Recepción crea ENTRADA automática | ✅ | `OrdenCompraService.registrarEntradaRecepcion()` en la misma `@Transactional` que el cambio a `RECIBIDA`. Test: `recepcion_creaMovimientoEntradaYSumaStock`. |
| 1.7 | Dashboard muestra indicadores y inventario actualizado | ✅ | `loadDashboard()` consume `/api/kpis`, riesgo, órdenes; refresca tras aprobar/recibir. |

---

## 2. Convenciones globales

| Requisito | Estado | Detalle |
| --- | --- | --- |
| Fuente de verdad = backend + BD | ✅ | Dashboard, MCP y n8n solo llaman REST. |
| Zona horaria `America/Bogota` | ✅ | `IndicadoresInventarioService.ZONA_BOGOTA`; KPIs `movimientosAyer`; validación fecha en `PanelResumenService`. |
| Errores 400/401/403/404 | ✅ | `GlobalExceptionHandler` + validaciones en servicios. |
| No stock negativo en salida/transferencia | ✅ | `MovimientoService` (reto anterior, conservado). |

---

## 3. Modelo mínimo

| Entidad / campo | Estado | Ubicación |
| --- | --- | --- |
| `Proveedor` (id, nombre, contacto, diasEntrega 1–90) | ✅ | `model/Proveedor.java`, tabla `proveedores`, `database/data.sql` |
| `Producto.proveedorPrincipal` opcional | ✅ | `model/Producto.java`, FK `proveedor_principal_id` |
| Sin proveedor → no riesgo ni orden auto | ✅ | `estaEnRiesgo(..., tieneProveedor)` en `IndicadoresInventarioService` |
| `OrdenCompra` (campos exigidos + PDF opcional) | ✅ | `model/OrdenCompra.java`, columnas `pdf` (BYTEA), `fecha_generacion_pdf` |
| `ResumenPanel` (un válido por fecha) | ✅ | `model/ResumenPanel.java`, unique en `fecha`; `PanelResumenService.publicar` |
| Rol `AGENTE` | ✅ | `model/Rol.java`, usuario `agente_mcp` en `data.sql` |

---

## 4. Indicadores y criterios fijos

| Indicador / regla | Fórmula PDF | Estado | Código |
| --- | --- | --- | --- |
| Ocupación por bodega | (uds/capacidad)×100 | ✅ | `obtenerKpis()` → `ocupacionPorBodega` |
| Productos en quiebre | stock total = 0 | ✅ | `productosEnQuiebre` |
| Productos en riesgo | con proveedor y stock < punto reorden | ✅ | `productosEnRiesgo` |
| Órdenes por aprobar | count BORRADOR + suma totales | ✅ | `ordenesPorAprobar` |
| Consumo diario promedio | salidas 30 días / 30 | ✅ | `consumoDiarioPromedio()` |
| Punto de reorden | consumo × diasEntrega × 1.5 | ✅ | `puntoReorden()` |
| Días cobertura | stock / consumo; null si consumo 0 | ✅ | `calcularCobertura()` → `SIN_CONSUMO` |
| Stock = punto reorden → no riesgo | estrictamente menor | ✅ | Test `stockIgualPuntoReorden_noEstaEnRiesgo` |
| Movimientos de ayer | ENTRADA/SALIDA/TRANSFERENCIA día anterior Bogotá | ✅ | En respuesta `GET /kpis` |
| Bodega crítica ≥ 90 % | ✅ | `listarBodegasCriticas()` |
| Bodega destino sugerida | menor stock; empate → menor id | ✅ | `sugerirBodegaDestinoId()` |

---

## 5. Estados de orden y PDF

| Requisito | Estado | Ubicación |
| --- | --- | --- |
| Transiciones BORRADOR→APROBADA/CANCELADA, etc. | ✅ | `OrdenCompraService.transicionPermitida()` |
| Transición inválida → 400 | ✅ | Test `ordenCancelada_noSeAprueba_retorna400` |
| RECIBIDA crea ENTRADA en una TX | ✅ | `cambiarEstado` + `registrarEntradaRecepcion` |
| POST/GET PDF, 404 si no generado | ✅ | `OrdenCompraController`, `OrdenPdfService` |
| Watermark diagonal BORRADOR | ✅ | `dibujarMarcaAguaDiagonal()` — tests `OrdenPdfServiceTest`, `pdfBorrador_watermarkYSeInvalidaAlCambiarEstado` |
| PDF se elimina al cambiar estado | ✅ | `cambiarEstado` pone `pdf = null` |
| Evidencia visual watermark | ✅ | `docs/evidencia/capturas/pdf-borrador-watermark.png` |

---

## 6. API requerida

Todos los endpoints IQ existen con **alias dual** (`/kpis` y `/api/kpis`, etc.) para compatibilidad con Swagger del reto anterior y MCP.

| Método y ruta | Estado | Controlador |
| --- | --- | --- |
| `GET /kpis` | ✅ | `KpiController` |
| `GET /productos/{id}/stock` | ✅ | `ProductoController` |
| `GET /productos/riesgo` | ✅ | `ProductoController` |
| `GET /bodegas/criticas` | ✅ | `BodegaController` |
| `GET /proveedores` | ✅ | `ProveedorController` |
| `GET /ordenes?estado=` | ✅ | `OrdenCompraController` |
| `POST /ordenes` | ✅ | `OrdenCompraController` |
| `GET /ordenes/{id}` | ✅ | `OrdenCompraController` |
| `POST/GET /ordenes/{id}/pdf` | ✅ | `OrdenCompraController` |
| `PATCH /ordenes/{id}/estado` | ✅ | `OrdenCompraController` (solo ADMIN) |
| `POST/GET /panel/resumen` | ✅ | `PanelResumenController` |
| Endpoints reto anterior conservados | ✅ | Bajo `/api/...` |
| Swagger/OpenAPI | ✅ | `http://localhost:8080/swagger-ui.html` |

---

## 7. Contrato resumen del panel

| Regla | Estado | Código |
| --- | --- | --- |
| Estructura estricta, sin propiedades extra | ✅ | `PanelResumenRequest` + `@JsonIgnoreProperties(ignoreUnknown = false)` |
| fecha = hoy Bogotá | ✅ | `validarContrato()` |
| narrativa 20–500 caracteres | ✅ | Bean Validation + servicio |
| Enumeraciones severidad/tipo | ✅ | Enums en DTO |
| IDs existentes | ✅ | `validarExistenciaIds()` |
| JSON inválido → 400, conserva anterior | ✅ | Test `resumenInvalido_conservaAnterior` |
| Un resumen por fecha, reemplazo + auditoría | ✅ | `publicar()` upsert + `AuditoriaService` |

---

## 8. Seguridad y auditoría

| Acción | AGENTE | ADMIN | Implementación |
| --- | --- | --- | --- |
| Consultar KPIs, stock, riesgo, críticas | Sí | Sí | `SecurityConfig` `hasAnyRole(ADMIN, AGENTE)` |
| Crear orden BORRADOR | Sí | Sí | POST `/ordenes` |
| Publicar resumen | Sí | Sí | POST `/panel/resumen` |
| Aprobar/recibir/cancelar | No | Sí | PATCH solo ADMIN; test `agenteAprueba_retorna403` |
| Movimientos manuales | No | Sí | POST `/api/movimientos` solo ADMIN |
| Auditoría de cambios de estado | ✅ | Creación orden, resumen, transiciones |

---

## 9. Servidor MCP (6 tools)

| Tool PDF | Estado | Archivo |
| --- | --- | --- |
| `consultar_stock_producto` | ✅ | `mcp-server/main.py` |
| `consultar_bodegas_criticas` | ✅ | idem |
| `consultar_productos_en_riesgo` | ✅ | idem |
| `consultar_kpis` | ✅ | idem |
| `crear_orden_borrador` | ✅ | idem |
| `publicar_resumen` | ✅ | idem |
| **No** tool de aprobar | ✅ | Confirmado en código y README MCP |
| JWT usuario AGENTE, sin MySQL | ✅ | Login en `main.py`; solo HTTP a API |

Evidencia por tool: `docs/evidencia/mcp-tools.md` y capturas en `docs/evidencia/capturas/`.

---

## 10. Skill y n8n

| Requisito | Estado | Ubicación |
| --- | --- | --- |
| `skills/operacion-logitrack/SKILL.md` | ✅ | Reglas: consultar primero, máx. 1 orden, no aprobar, JSON válido, informar errores |
| Flujo único *Resumen diario de inventario* | ✅ | `n8n/resumen-diario-inventario.json` |
| Cron 06:00 America/Bogota | ✅ | `0 6 * * *`, timezone workflow |
| Cantidad `ceil(max(1, puntoReorden×2 - stockTotal))` | ✅ | Documentado en SKILL y prompt del AI Agent en JSON n8n |
| Éxito y error controlado | ✅ | `docs/evidencia/n8n-ejecucion.md` |

---

## 11. Dashboard web

| Requisito PDF | Estado | Notas |
| --- | --- | --- |
| HTML/CSS/JS sin framework | ✅ | `static/index.html`, `style.css`, `app.js` |
| Carpeta `frontend/` | ⚠️ | UI en `logitrack/.../static/`; ver `frontend/README.md` |
| Cuatro indicadores + ocupación + ayer | ✅ | `renderKpis()` en `app.js` |
| Narrativa, alertas, acciones del resumen | ✅ | `renderPanelResumen()` |
| Productos en riesgo y órdenes BORRADOR | ✅ | Tablas en dashboard |
| Generar y ver PDF BORRADOR con watermark | ✅ | `generarPdfOrden`, `verPdfOrden` |
| Login JWT reutilizado | ✅ | `POST /auth/login` |
| JWT solo en `sessionStorage` | ✅ | `app.js` líneas 19–42 |
| Botón Aprobar solo ADMIN | ✅ | `botonesEstado()` + `isAdmin()` |
| Tabla actualizada tras aprobar | ✅ | `cambiarEstadoOrden` llama `loadDashboard()` |
| Interfaz móvil no calificada | — | Se implementó responsive legible (ver `frontend-dashboard.md`) |

---

## 12. Pruebas obligatorias

| # | Caso PDF | Prueba | Archivo |
| --- | --- | --- | --- |
| 1 | Consumo 0 → SIN_CONSUMO | `consumoCero_coberturaNullYSinConsumo` | `IndicadoresInventarioServiceTest` |
| 2 | Stock = punto reorden → no riesgo | `stockIgualPuntoReorden_noEstaEnRiesgo` | idem |
| 3 | Cantidad ≤ 0 → 400 | `ordenCantidadInvalida_retorna400` | `OrdenCompraEstadoTest` |
| 4 | Cancelada no se aprueba → 400 | `ordenCancelada_noSeAprueba_retorna400` | idem |
| 5 | Recibida genera ENTRADA | `recepcion_creaMovimientoEntradaYSumaStock` | idem |
| 6 | AGENTE aprueba → 403 | `agenteAprueba_retorna403` | idem |
| 7 | Resumen inválido conserva anterior | `resumenInvalido_conservaAnterior` | `PanelResumenTest` |
| 8 | PDF BORRADOR + invalidación | `pdfBorrador_watermarkYSeInvalidaAlCambiarEstado` | `OrdenCompraEstadoTest` |
| 9 | Integración PATCH o POST panel | Ambos en paquete `iq/` | ✅ |

Pruebas escritas antes de implementar: commits `48ab691` (test) → `cddb2ca` (feat). Ver `evidencia-sdd.md`.

---

## 13. Proceso SDD y entregables

| Entregable PDF | Estado | Ubicación |
| --- | --- | --- |
| `docs/sdd/` (01–04) | ✅ | `docs/sdd/` |
| `evidencia-sdd.md` con hashes y regla→prueba | ✅ | `docs/sdd/evidencia-sdd.md` |
| README instalación y usuarios | ✅ | `README.md`, `logitrack/README.md` |
| `schema.sql` / `data.sql` | ✅ | `logitrack/database/` |
| Diagrama n8n→MCP→API→BD→dashboard | ✅ | `docs/diagrama-arquitectura.md` |
| Export n8n + capturas éxito/error | ✅ | `n8n/` + `docs/evidencia/` |
| Video 4–6 min | ⬜ | Pendiente (externo al repo) |

---

## 14. Divergencias aceptadas (con justificación)

| Tema | PDF / enunciado | Implementación | Por qué es aceptable |
| --- | --- | --- | --- |
| Base de datos | Menciona MySQL como “no tocar directamente” | PostgreSQL (`schema.sql`) | El reto permite adaptar estructura al proyecto anterior; Supabase/PostgreSQL es el stack real del curso previo. |
| Carpeta `frontend/` | Raíz `frontend/` | `logitrack/src/main/resources/static/` | Spring Boot sirve UI y API en `:8080`; mismo HTML/CSS/JS sin framework; responsabilidades separadas en documentación. |
| Prefijo `/api` | Rutas sin prefijo en tabla PDF | Alias `/api/*` + rutas sin prefijo | Compatibilidad con reto anterior y MCP; ambas funcionan. |
| Rol `EMPLEADO` | No en tabla IQ | Conservado del reto 1 | No contradice reglas AGENTE/ADMIN. |
| Diseño móvil avanzado | No calificado | Responsive básico implementado | Mejora legibilidad sin ser requisito de nota. |

---

## Resumen ejecutivo

**El proyecto cumple el flujo completo y los requisitos obligatorios del PDF** (modelo, API, estados, PDF, MCP, skill, n8n, dashboard, pruebas, SDD). Las únicas divergencias estructurales son PostgreSQL en lugar de MySQL y la ubicación del frontend dentro de `static/`, ambas documentadas y sin cambiar el comportamiento esperado. Pendiente: video de demostración (entregable humano).
