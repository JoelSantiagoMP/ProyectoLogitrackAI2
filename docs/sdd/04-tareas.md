# 04 — Tareas (rúbrica y entregables)

Casillas según el proceso SDD/TDD del reto. Las pruebas de reglas nuevas se escriben **antes** de implementar esas reglas.

## A. SDD, Git y documentación (criterio 5)

- [x] Commit diario; primer día: repositorio y este conjunto en `docs/`
- [x] Completar `01-propuesta.md`, `02-especificacion.md`, `03-diseno.md`, `04-tareas.md`
- [x] Completar `evidencia-sdd.md` (enlaces, tabla regla → prueba, hashes de 3 commits, rojo/verde, reflexión ≤ 150 palabras)
- [x] Commit SDD inicial (`e38bcbc`; mensaje equivalente al exigido)
- [x] README: instalación, ejecución, usuarios de prueba, rutas principales (ver `logitrack/README.md`)
- [x] Swagger/OpenAPI de endpoints nuevos y evidencia de endpoints protegidos
- [x] Diagrama n8n → MCP → API Spring Boot → MySQL → dashboard (`docs/diagrama-arquitectura.md`)

## B. Modelo, reglas y pruebas (criterio 1) — TDD

- [x] Commit `test: define reorder and order-state rules` (`48ab691`)
- [x] Prueba: consumo 0 → cobertura `null` y `SIN_CONSUMO`
- [x] Prueba: stock igual al punto de reorden → no está en riesgo
- [x] Prueba: cantidad 0 o negativa → `400`
- [x] Prueba: orden `CANCELADA` no se aprueba → `400`
- [x] Prueba: `APROBADA` → `RECIBIDA` genera `ENTRADA`
- [x] Prueba: `AGENTE` aprueba → `403`
- [x] Prueba: resumen severidad inválida o ID inexistente → `400` y se conserva el anterior
- [x] Prueba: PDF `BORRADOR` guardado con marca de agua; al cambiar estado deja de estar disponible
- [x] Al menos una prueba de integración: `PATCH /ordenes/{id}/estado` o `POST /panel/resumen`
- [x] Entidad `Proveedor` (`diasEntrega` 1–90) y datos reproducibles
- [x] `Producto.proveedorPrincipal` opcional
- [x] Entidad `OrdenCompra` y transiciones de estado
- [x] Entidad `ResumenPanel` (un válido por fecha, reemplazo + auditoría)
- [x] Rol `AGENTE` y matriz de permisos
- [x] Stock y KPIs; zona `America/Bogota`
- [x] Recepción transaccional (orden + `ENTRADA`)
- [x] Commit `feat: implement LogiTrack IQ rules` (`cddb2ca`)

## C. API

- [x] `GET /kpis` (cuatro indicadores, ayer, `calculadoEn`)
- [x] `GET /productos/{id}/stock` (alias `/api/...` y sin prefijo)
- [x] `GET /productos/riesgo` (campos y `bodegaDestinoId` sugerida)
- [x] `GET /bodegas/criticas` (≥ 90 %)
- [x] `GET /proveedores`
- [x] `GET /ordenes` con filtro `estado`
- [x] `POST /ordenes` (`BORRADOR`, `total` en servidor)
- [x] `GET /ordenes/{id}`
- [x] `POST` y `GET /ordenes/{id}/pdf` (`404` si no existe; watermark; borrar PDF al cambiar estado)
- [x] `PATCH /ordenes/{id}/estado` con cuerpo `{ "estado": "..." }`
- [x] `POST` y `GET /panel/resumen` (contrato estricto)
- [x] Conservar endpoints del reto anterior
- [x] `schema.sql` / `data.sql` (o equivalente) con `ENTRADA` inicial para pruebas

## D. MCP, skill y n8n (criterio 3)

- [x] `mcp-server/` con JWT de `AGENTE` (sin MySQL, sin reglas de negocio)
- [x] Tool `consultar_stock_producto`
- [x] Tool `consultar_bodegas_criticas`
- [x] Tool `consultar_productos_en_riesgo`
- [x] Tool `consultar_kpis`
- [x] Tool `crear_orden_borrador`
- [x] Tool `publicar_resumen`
- [x] Confirmar que **no** existe tool de aprobar
- [x] Evidencia entrada/respuesta de cada herramienta → [`docs/evidencia/mcp-tools.md`](../evidencia/mcp-tools.md)
- [x] `skills/operacion-logitrack/SKILL.md` (consultas primero, máx. 1 orden, no aprobar/recibir, JSON válido, error explícito)
- [x] Flujo único `n8n/resumen-diario-inventario.json` (cron 6:00 `America/Bogota`)
- [x] Cantidad `ceil(max(1, puntoReorden × 2 - stockTotal))` para el primer producto en riesgo
- [x] Captura de ejecución exitosa → [`docs/evidencia/n8n-ejecucion.md`](../evidencia/n8n-ejecucion.md) sección A
- [x] Captura de error controlado **sin** crear una orden indebida → sección B

## E. Dashboard y PDF (criterio 4)

- [x] Dashboard HTML/CSS/JS sin framework (`logitrack/src/main/resources/static/`)
- [x] Login JWT reutilizado; token solo en `sessionStorage`
- [x] Cuatro indicadores, ocupación por bodega, movimientos de ayer
- [x] Narrativa, alertas y acciones del último resumen
- [x] Tabla de productos en riesgo y órdenes `BORRADOR`
- [x] Generar y visualizar PDF `BORRADOR` con marca de agua diagonal
- [x] Botón Aprobar solo para `ADMIN`; tabla se actualiza al aprobar
- [x] Evidencia de PDF guardado con watermark → [`docs/evidencia/pdf-borrador.md`](../evidencia/pdf-borrador.md)

## F. Video y flujo completo (criterio 2)

- [ ] Video 4–6 minutos **sin** mostrar ni explicar código
- [ ] Datos iniciales + ejecución manual del flujo n8n
- [ ] Consulta de riesgo y creación de orden `BORRADOR`
- [ ] Aprobación por `ADMIN`
- [ ] Recepción, movimiento `ENTRADA` y dashboard actualizado
- [ ] Narración clara de la decisión de compra
