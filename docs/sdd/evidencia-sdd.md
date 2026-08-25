# Evidencia SDD / TDD — LogiTrack IQ

Esta carpeta certifica de forma verificable el uso de SDD y TDD. No basta con afirmarlo en el README.

## Documentos SDD

- [01 — Propuesta](01-propuesta.md)
- [02 — Especificación](02-especificacion.md)
- [03 — Diseño](03-diseno.md)
- [04 — Tareas](04-tareas.md)

## Commits obligatorios (en este orden)

Mensajes exactos exigidos por el reto. Completar el hash cuando exista cada commit.

| # | Mensaje | Hash |
| --- | --- | --- |
| 1 | `docs: define LogiTrack IQ scope` | `_pendiente_` |
| 2 | `test: define reorder and order-state rules` | `_pendiente_` |
| 3 | `feat: implement LogiTrack IQ rules` | `_pendiente_` |

```text
1. docs: define LogiTrack IQ scope
   hash: ________________________________

2. test: define reorder and order-state rules
   hash: ________________________________

3. feat: implement LogiTrack IQ rules
   hash: ________________________________
```

## Tabla regla → prueba

| # | Regla (especificación) | Prueba (nombre tentativo / clase) | Estado |
| --- | --- | --- | --- |
| 1 | Consumo 0: cobertura `null` y estado `SIN_CONSUMO` | `_pendiente: consumoCero_coberturaNullYSinConsumo_` | [ ] rojo → [ ] verde |
| 2 | Stock igual al punto de reorden: no está en riesgo | `_pendiente: stockIgualPuntoReorden_noEstaEnRiesgo_` | [ ] rojo → [ ] verde |
| 3 | Cantidad 0 o negativa: `400` | `_pendiente: ordenCantidadInvalida_retorna400_` | [ ] rojo → [ ] verde |
| 4 | Orden `CANCELADA`: no se puede aprobar (`400`) | `_pendiente: ordenCancelada_noSeAprueba_` | [ ] rojo → [ ] verde |
| 5 | Orden `APROBADA` recibida: genera `ENTRADA` | `_pendiente: recepcion_creaMovimientoEntrada_` | [ ] rojo → [ ] verde |
| 6 | `AGENTE` intenta aprobar: `403` | `_pendiente: agenteAprueba_retorna403_` | [ ] rojo → [ ] verde |
| 7 | Resumen severidad inválida o ID inexistente: `400` y se conserva el resumen anterior | `_pendiente: resumenInvalido_conservaAnterior_` | [ ] rojo → [ ] verde |
| 8 | PDF `BORRADOR` se guarda con marca de agua; al cambiar estado deja de estar disponible | `_pendiente: pdfBorrador_watermarkYSeInvalidaAlCambiarEstado_` | [ ] rojo → [ ] verde |
| 9 | Integración `PATCH /ordenes/{id}/estado` **o** `POST /panel/resumen` | `_pendiente: integracion_patchEstado_o_postResumen_` | [ ] rojo → [ ] verde |

## Evidencia de prueba inicial fallando (rojo)

Fecha: `_pendiente_`

Comando:

```text
_pendiente: ./mvnw test  (o comando equivalente)
```

Resultado esperado en esta etapa: fallan las pruebas de reglas IQ **aún no implementadas**.

Salida / captura: `_adjuntar log o ruta de evidencia_`

## Evidencia de ejecución final en verde

Fecha: `_pendiente_`

Comando:

```text
_pendiente: ./mvnw test
```

Resultado esperado: suite de reglas IQ en verde.

Salida / captura: `_adjuntar log o ruta de evidencia_`

## Reflexión (máximo 150 palabras)

Completar **después** de implementar, o escribir exactamente: `No hubo cambios`.

```text
_pendiente: cambios entre especificación e implementación, o «No hubo cambios».
```
