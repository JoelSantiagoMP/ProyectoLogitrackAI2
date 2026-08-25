# 04 — Tareas (rúbrica y entregables)

Casillas según el proceso SDD/TDD del reto. Las pruebas de reglas nuevas se escriben **antes** de implementar esas reglas.

## A. SDD, Git y documentación (criterio 5)

- [ ] Commit diario; primer día: repositorio y este conjunto en `docs/`
- [ ] Completar `01-propuesta.md`, `02-especificacion.md`, `03-diseno.md`, `04-tareas.md`
- [ ] Completar `evidencia-sdd.md` (enlaces, tabla regla → prueba, hashes de 3 commits, rojo/verde, reflexión ≤ 150 palabras)
- [ ] Commit `docs: define LogiTrack IQ scope`
- [ ] README: instalación, ejecución, usuarios de prueba, rutas principales
- [ ] Swagger/OpenAPI de endpoints nuevos y evidencia de endpoints protegidos
- [ ] Diagrama n8n → MCP → API Spring Boot → MySQL → dashboard

## B. Modelo, reglas y pruebas (criterio 1) — TDD

- [ ] Commit `test: define reorder and order-state rules` (pruebas en rojo)
- [ ] Prueba: consumo 0 → cobertura `null` y `SIN_CONSUMO`
- [ ] Prueba: stock igual al punto de reorden → no está en riesgo
- [ ] Prueba: cantidad 0 o negativa → `400`
- [ ] Prueba: orden `CANCELADA` no se aprueba → `400`
- [ ] Prueba: `APROBADA` → `RECIBIDA` genera `ENTRADA`
- [ ] Prueba: `AGENTE` aprueba → `403`
- [ ] Prueba: resumen severidad inválida o ID inexistente → `400` y se conserva el anterior
- [ ] Prueba: PDF `BORRADOR` guardado con marca de agua; al cambiar estado deja de estar disponible
- [ ] Al menos una prueba de integración: `PATCH /ordenes/{id}/estado` o `POST /panel/resumen`
- [ ] Entidad `Proveedor` (`diasEntrega` 1–90) y datos reproducibles
- [ ] `Producto.proveedorPrincipal` opcional
- [ ] Entidad `OrdenCompra` y transiciones de estado
- [ ] Entidad `ResumenPanel` (un válido por fecha, reemplazo + auditoría)
- [ ] Rol `AGENTE` y matriz de permisos
- [ ] Stock y KPIs solo desde movimientos; zona `America/Bogota`
- [ ] Recepción transaccional (orden + `ENTRADA`)
- [ ] Commit `feat: implement LogiTrack IQ rules` (pruebas en verde)

## C. API

- [ ] `GET /kpis` (cuatro indicadores, ayer, `calculadoEn`)
- [ ] `GET /productos/{id}/stock`
- [ ] `GET /productos/riesgo` (campos y `bodegaDestinoId` sugerida)
- [ ] `GET /bodegas/criticas` (≥ 90 %)
- [ ] `GET /proveedores`
- [ ] `GET /ordenes` con filtro `estado`
- [ ] `POST /ordenes` (`BORRADOR`, `total` en servidor)
- [ ] `GET /ordenes/{id}`
- [ ] `POST` y `GET /ordenes/{id}/pdf` (`404` si no existe; watermark; borrar PDF al cambiar estado)
- [ ] `PATCH /ordenes/{id}/estado` con cuerpo `{ "estado": "..." }`
- [ ] `POST` y `GET /panel/resumen` (contrato estricto)
- [ ] Conservar endpoints del reto anterior
- [ ] `schema.sql` / `data.sql` (o equivalente) con `ENTRADA` inicial para pruebas

## D. MCP, skill y n8n (criterio 3)

- [ ] `mcp-server/` con JWT de `AGENTE` (sin MySQL, sin reglas de negocio)
- [ ] Tool `consultar_stock_producto`
- [ ] Tool `consultar_bodegas_criticas`
- [ ] Tool `consultar_productos_en_riesgo`
- [ ] Tool `consultar_kpis`
- [ ] Tool `crear_orden_borrador`
- [ ] Tool `publicar_resumen`
- [ ] Confirmar que **no** existe tool de aprobar
- [ ] Evidencia entrada/respuesta de cada herramienta
- [ ] `skills/operacion-logitrack/SKILL.md` (consultas primero, máx. 1 orden, no aprobar/recibir, JSON válido, error explícito)
- [ ] Flujo único `n8n/resumen-diario-inventario.json` (cron 6:00 `America/Bogota`)
- [ ] Cantidad `ceil(max(1, puntoReorden × 2 - stockTotal))` para el primer producto en riesgo
- [ ] Captura de ejecución exitosa
- [ ] Captura de error controlado **sin** crear una orden indebida

## E. Dashboard y PDF (criterio 4)

- [ ] `frontend/` HTML/CSS/JS sin framework
- [ ] Login JWT reutilizado; token solo en `sessionStorage`
- [ ] Cuatro indicadores, ocupación por bodega, movimientos de ayer
- [ ] Narrativa, alertas y acciones del último resumen
- [ ] Tabla de productos en riesgo y órdenes `BORRADOR`
- [ ] Generar y visualizar PDF `BORRADOR` con marca de agua diagonal
- [ ] Botón Aprobar solo para `ADMIN`; tabla se actualiza al aprobar
- [ ] Evidencia de PDF guardado con watermark

## F. Video y flujo completo (criterio 2)

- [ ] Video 4–6 minutos **sin** mostrar ni explicar código
- [ ] Datos iniciales + ejecución manual del flujo n8n
- [ ] Consulta de riesgo y creación de orden `BORRADOR`
- [ ] Aprobación por `ADMIN`
- [ ] Recepción, movimiento `ENTRADA` y dashboard actualizado
- [ ] Narración clara de la decisión de compra
