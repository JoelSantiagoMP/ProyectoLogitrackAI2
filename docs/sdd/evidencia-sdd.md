# Evidencia SDD / TDD — LogiTrack IQ

Esta carpeta certifica de forma verificable el uso de SDD y TDD.

## Documentos SDD

- [01 — Propuesta](01-propuesta.md)
- [02 — Especificación](02-especificacion.md)
- [03 — Diseño](03-diseno.md)
- [04 — Tareas](04-tareas.md)
- [Diagrama de arquitectura](../diagrama-arquitectura.md)
- [Índice de documentación](../README.md)
- [Alineación requisitos PDF](../alineacion-requisitos-pdf.md)
- [Wireframes](../wireframes.md)
- [Dashboard y responsive](../frontend-dashboard.md)
- [Arquitectura en código](../arquitectura-codigo.md)
- [Evidencia operativa (MCP, n8n, PDF)](../evidencia/README.md)

## Commits obligatorios (en este orden)

| # | Mensaje exigido | Hash en repositorio | Nota |
| --- | --- | --- | --- |
| 1 | `docs: define LogiTrack IQ scope` | `e38bcbc` | Mensaje real: `feat: add SDD documentation for LogiTrack IQ project...` |
| 2 | `test: define reorder and order-state rules` | `48ab691` | |
| 3 | `feat: implement LogiTrack IQ rules` | `cddb2ca` | |

```text
1. docs: define LogiTrack IQ scope
   hash: e38bcbca4fb3a9fc44ca930ea88f29050c3108f3

2. test: define reorder and order-state rules
   hash: 48ab691a6d0556a1fd1a8e0d047deeee81d4de1a

3. feat: implement LogiTrack IQ rules
   hash: cddb2ca3bc69dc3679a94b3fb3e8f10f3a13d65a
```

## Tabla regla → prueba

| # | Regla (especificación) | Prueba | Estado |
| --- | --- | --- | --- |
| 1 | Consumo 0: cobertura `null` y estado `SIN_CONSUMO` | `IndicadoresInventarioServiceTest.consumoCero_coberturaNullYSinConsumo` | [x] verde |
| 2 | Stock igual al punto de reorden: no está en riesgo | `IndicadoresInventarioServiceTest.stockIgualPuntoReorden_noEstaEnRiesgo` | [x] verde |
| 3 | Cantidad 0 o negativa: `400` | `OrdenCompraEstadoTest.ordenCantidadInvalida_retorna400` | [x] verde |
| 4 | Orden `CANCELADA`: no se puede aprobar (`400`) | `OrdenCompraEstadoTest.ordenCancelada_noSeAprueba_retorna400` | [x] verde |
| 5 | Orden `APROBADA` recibida: genera `ENTRADA` | `OrdenCompraEstadoTest.recepcion_creaMovimientoEntrada` | [x] verde |
| 6 | `AGENTE` intenta aprobar: `403` | `OrdenCompraEstadoTest.agenteAprueba_retorna403` | [x] verde |
| 7 | Resumen severidad inválida o ID inexistente: `400` y se conserva el anterior | `PanelResumenTest.resumenInvalido_conservaAnterior` | [x] verde |
| 8 | PDF `BORRADOR` se guarda con marca de agua; al cambiar estado deja de estar disponible | `OrdenCompraEstadoTest.pdfBorrador_watermarkYSeInvalidaAlCambiarEstado`, `OrdenPdfServiceTest` | [x] verde |
| 9 | Integración `PATCH /ordenes/{id}/estado` **o** `POST /panel/resumen` | `OrdenCompraEstadoTest`, `PanelResumenTest` | [x] verde |
| — | `AGENTE` no registra movimientos: `403` | `OrdenCompraEstadoTest.agenteRegistraMovimiento_retorna403` | [x] verde |

## Evidencia de prueba inicial fallando (rojo)

Fecha: 2026-08-25 (commit `48ab691`, antes de `feat: implement LogiTrack IQ rules`)

Comando:

```bash
cd logitrack && ./mvnw test
```

Resultado esperado en esa etapa: las pruebas de reglas IQ en el paquete `com.example.logitrack.iq` fallaban porque la implementación aún no existía (TDD).

## Evidencia de ejecución final en verde

Fecha: 2026-08-25

Comando:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd logitrack && ./mvnw test
```

Resultado (última ejecución verificada):

```text
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Log completo: `logitrack/target/surefire-reports/` (generado al ejecutar `./mvnw test`).

## Reflexión (máximo 150 palabras)

Se mantuvo la estructura del backend anterior con prefijo `/api/` y se añadieron alias sin prefijo para rutas IQ. El stock operativo sigue materializándose en `inventario_bodega` al registrar movimientos. Los nombres de las tools MCP se alinearon con el PDF. El dashboard vive en `static/` en lugar de `frontend/` en la raíz; la carpeta `frontend/README.md` y `docs/frontend-dashboard.md` documentan esa decisión y el comportamiento responsive. La BD es PostgreSQL, no MySQL, con scripts equivalentes en `database/`.
