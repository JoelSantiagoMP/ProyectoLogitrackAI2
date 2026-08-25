# 01 — Propuesta: LogiTrack IQ

Documento SDD derivado estrictamente del reto **Proyecto integrador IA2 — LogiTrack IQ: Torre de control de inventario**.

## Problema

LogiTrack S.A. ya dispone de un backend Spring Boot para bodegas, productos y movimientos de inventario. La información existe, pero se revisa de forma manual y **no hay una vista diaria** que ayude a detectar faltantes ni a preparar una compra.

La información se consulta de forma aislada; no hay un flujo que identifique un producto en riesgo, prepare una orden, registre su recepción y muestre el resultado en un tablero.

## Objetivo general

Integrar Spring Boot, pruebas, SDD, MCP, skills y n8n en una solución pequeña que:

- monitoree inventario;
- proponga una compra en **borrador**;
- entregue información clara a un administrador.

Este proyecto **extiende** el reto anterior de LogiTrack. No se crea un backend independiente ni se reemplazan las funciones ya construidas.

## Objetivo de demostración (flujo de punta a punta)

Al finalizar debe poder demostrarse, con datos reales del sistema:

1. Existe un producto en riesgo.
2. n8n consulta el backend mediante MCP y crea una orden en `BORRADOR`.
3. Un `ADMIN` aprueba y recibe esa orden.
4. La recepción crea un movimiento `ENTRADA`.
5. El dashboard refleja la orden y el inventario actualizado.

Si este flujo funciona, está probado y se evidencia en el video, el objetivo principal del proyecto está cumplido.

## Qué sistema se debe construir

Extensión de LogiTrack que complete este flujo de negocio:

1. El sistema calcula el inventario real a partir de los movimientos registrados.
2. Detecta productos cuyo stock está por debajo de su **punto de reorden**.
3. El flujo diario de n8n consulta esa información mediante MCP y crea, **como máximo, una** orden de compra en estado `BORRADOR`.
4. Un administrador revisa la orden en el dashboard y la aprueba.
5. Cuando la orden se recibe, el backend registra automáticamente una **entrada** de inventario en la bodega indicada.
6. El dashboard muestra indicadores, alertas, órdenes pendientes e inventario actualizado.

En pocas palabras: el sistema detecta un faltante, prepara una compra, permite recibirla y demuestra que el inventario se actualizó.

## Alcance (obligatorio — “debe”)

Cuando el documento del reto dice **debe**, es obligatorio y se califica.

- Extender el backend existente (no sustituirlo).
- Modelo mínimo: `Proveedor`, `Producto.proveedorPrincipal`, `OrdenCompra`, `ResumenPanel`, rol `AGENTE`.
- Indicadores fijos del dashboard, punto de reorden y ocupación crítica (≥ 90 %).
- Estados de orden y recepción transaccional (`APROBADA` → `RECIBIDA` crea `ENTRADA`).
- API nueva documentada en Swagger/OpenAPI, conservando los endpoints del reto anterior.
- PDF de orden con marca de agua `BORRADOR` cuando aplique.
- Contrato estricto de `POST /panel/resumen` y `GET /panel/resumen`.
- JWT, usuarios y auditoría reutilizados; auditoría de cambios de estado.
- Servidor MCP con **exactamente seis** herramientas; sin herramienta de aprobación.
- Skill `skills/operacion-logitrack/SKILL.md` y un único flujo n8n **Resumen diario de inventario**.
- Dashboard en `frontend/` (HTML, CSS y JS sin framework).
- Pruebas de reglas nuevas escritas **antes** de implementar esas reglas, más al menos una prueba de integración.
- Documentación SDD en `docs/sdd/` y evidencia TDD.
- Zona horaria `America/Bogota` en backend, n8n y datos de prueba.
- Fuente de verdad: backend y base de datos. Dashboard, MCP y n8n consultan o usan la API; **no** calculan ni modifican datos directamente en MySQL.

## Fuera de alcance

- Crear un backend independiente o reemplazar bodegas, productos, movimientos, login JWT o auditoría ya construidos.
- Que el dashboard, MCP o n8n calculen o escriban inventario directamente en MySQL.
- Herramienta MCP para aprobar, cancelar o recibir órdenes.
- Más de una orden automática por ejecución del flujo n8n.
- Cargar dinámicamente `SKILL.md` desde n8n (el archivo es evidencia mantenible; las reglas se copian o adaptan al nodo AI Agent).
- Validar el significado en lenguaje natural de la `narrativa` del resumen (sí se valida estructura, longitudes, enumeraciones e IDs).
- Auditar consultas (GET); solo son obligatorias las acciones que cambian el estado del sistema.
- Animaciones, interfaz móvil o diseño avanzado del dashboard (sí se califica legibilidad y consumo de endpoints reales).
- La orden de compra **no** es un PDF por sí misma: el PDF es un documento generado desde una orden ya guardada.
- Librería externa de JSON Schema (se permite DTO + Bean Validation + comprobaciones de servicio).
- En el video: mostrar o explicar código.

## Convenciones del reto (aplican a todo el proyecto)

- **Estados y permisos:** una orden solo cambia con las transiciones y roles definidos. Que una acción aparezca en el dashboard no implica que todos los usuarios puedan ejecutarla.
- **Errores:** se reutiliza el manejo global de excepciones del backend anterior. Validaciones y transiciones inválidas: `400`; recursos inexistentes: `404`; acciones prohibidas por rol: `403`; sesión no válida: `401`.
- **Evidencia funcional:** una captura, un DTO o un endpoint documentado no sustituyen una ejecución real.
- **“Puede”:** alternativa de implementación que no cambia el comportamiento esperado.
