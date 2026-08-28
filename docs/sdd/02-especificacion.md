# 02 — Especificación: reglas, API y contrato del panel

Zona horaria de backend, n8n y datos de prueba: **`America/Bogota`**.

Fuente de verdad: el backend y su base de datos (PostgreSQL en este proyecto). Dashboard, MCP y n8n no calculan ni modifican datos en la BD directamente.

## 1. Reglas base de inventario

- La capacidad de una bodega se mide en **unidades de producto** y debe ser **mayor que 0**.
- El stock se calcula a partir de los **movimientos**. El campo `Producto.stock` del reto anterior **no** es fuente para estos cálculos nuevos.
- Un movimiento puede tener uno o varios detalles de producto. Cada cálculo debe recorrer sus detalles. Si la implementación anterior guarda un solo producto por movimiento, se aplica la misma regla a ese producto.
- Al iniciar las pruebas, los datos deben incluir movimientos `ENTRADA` que representen inventario inicial.
- `ENTRADA` suma unidades a la bodega destino.
- `SALIDA` resta unidades a la bodega origen.
- `TRANSFERENCIA` resta en origen y suma la misma cantidad en destino.
- No se permite una salida o transferencia que deje una bodega con **stock negativo**.
- El stock total de un producto es la **suma** de sus existencias en todas las bodegas.

## 2. Punto de reorden y productos en riesgo

Para cada producto (y en la tabla de riesgo del dashboard):

| Dato | Regla exacta |
| --- | --- |
| Consumo diario promedio | Unidades en movimientos `SALIDA` de los **últimos 30 días calendario**, **incluida** la fecha de consulta, dividido entre **30**. |
| Punto de reorden | `consumo diario promedio × diasEntrega × 1.5` |
| Días de cobertura | `stock total / consumo diario promedio` |
| Consumo = 0 | Cobertura = `null` y estado mostrado = `SIN_CONSUMO` |
| ¿Está en riesgo? | Tiene **proveedor principal** y `stock total < punto de reorden`. Si el stock es **igual** al punto de reorden, **no** está en riesgo. |
| Producto sin proveedor principal | No puede aparecer como producto en riesgo ni generar una orden automática. |

`diasEntrega` del proveedor es un entero entre **1 y 90**.

**Bodega destino sugerida** (`bodegaDestinoId`): la bodega con el **menor stock** de ese producto; en empate, la de **menor id**.

## 3. Indicadores del dashboard (cuatro tarjetas)

| Indicador | Regla exacta |
| --- | --- |
| Ocupación por bodega | `(unidades almacenadas en la bodega / capacidad) × 100`. Se muestra por cada bodega. |
| Productos en quiebre | Cantidad de productos cuyo **stock total es 0**. |
| Productos en riesgo | Cantidad de productos **con proveedor principal** cuyo stock total es **menor** que su punto de reorden. |
| Órdenes por aprobar | Cantidad de órdenes en `BORRADOR` y **suma de sus totales**. |

**Movimientos de ayer:** conteo separado de `ENTRADA`, `SALIDA` y `TRANSFERENCIA` del **día calendario anterior** en `America/Bogota`. Bloque informativo, no tarjeta principal.

**Bodega crítica:** ocupación **mayor o igual a 90 %**.

## 4. Orden de compra: estados y transiciones

Estados permitidos: `BORRADOR`, `APROBADA`, `RECIBIDA`, `CANCELADA`.

`BORRADOR` es el estado inicial: la orden existe en la base de datos, pero todavía no ha sido aprobada ni recibida.

| Estado actual | Siguiente estado permitido |
| --- | --- |
| `BORRADOR` | `APROBADA` o `CANCELADA` |
| `APROBADA` | `RECIBIDA` o `CANCELADA` |
| `RECIBIDA` | Ninguno |
| `CANCELADA` | Ninguno |

Una transición no listada responde **`400 Bad Request`** con un mensaje claro.

Al pasar de `APROBADA` a `RECIBIDA`, el sistema crea automáticamente un movimiento `ENTRADA` para su producto, cantidad y `bodegaDestino`. La actualización de la orden y la creación del movimiento ocurren en **una sola transacción**: ambas se completan o ninguna se guarda.

La orden tiene exactamente un producto y una cantidad **mayor que 0**. `bodegaDestino` es obligatoria. `POST /ordenes` calcula `total` en el servidor.

## 5. PDF de la orden

- `POST /ordenes/{id}/pdf` genera el PDF y lo guarda asociado a la orden. Si ya existe, lo **reemplaza**.
- Debe incluir: número de orden, fecha de creación, proveedor, producto, cantidad, precio unitario, total, bodega destino y estado.
- En estado `BORRADOR`: marca de agua **diagonal**, semitransparente y legible con el texto `BORRADOR`.
- `GET /ordenes/{id}/pdf` entrega `application/pdf`. Si aún no se ha generado, **`404`**.
- Al **cambiar el estado** de una orden, el PDF guardado se **elimina**. Debe generarse de nuevo para reflejar el estado actual.

## 6. Resumen del panel

- `POST /panel/resumen` valida y publica un resumen estructurado.
- `GET /panel/resumen` devuelve el último resumen válido o **`404`** si no existe.
- Solo puede haber **un resumen válido por fecha**. Una nueva publicación para la misma fecha **reemplaza** el contenido anterior y queda registrada en auditoría.
- JSON inválido: **`400`** y el último resumen válido **permanece** disponible.
- `fecha` usa `YYYY-MM-DD` y corresponde a la **fecha actual** en `America/Bogota`.
- `narrativa`: entre **20 y 500** caracteres.
- `alertas` y `accionesSugeridas` son arreglos, aunque estén vacíos.
- `severidad`: `BAJA`, `MEDIA` o `ALTA`.
- `tipo`: `REVISAR_ORDEN`, `REVISAR_PRODUCTO` o `REVISAR_BODEGA`.
- Cada identificador informado debe existir.
- Una alerta enlaza **al menos un** identificador; una acción enlaza **exactamente uno**.
- No se admite propiedades adicionales.
- No se exige validar el significado de la narrativa en lenguaje natural.

## 7. Seguridad y auditoría

Se reutilizan JWT, usuarios y auditoría del proyecto anterior. Rol nuevo: **`AGENTE`**.

| Acción | AGENTE | ADMIN |
| --- | --- | --- |
| Consultar KPIs, stock, riesgos y bodegas críticas | Sí | Sí |
| Crear orden en `BORRADOR` | Sí | Sí |
| Publicar resumen | Sí | Sí |
| Aprobar, recibir o cancelar una orden | No | Sí |
| Registrar movimientos manualmente | No | Sí |

Códigos: `400` validación/transición inválida; `404` recurso inexistente; `403` rol; `401` sesión no válida.

Auditoría obligatoria al cambiar estado: creación de orden, publicación/reemplazo de resumen, transición de orden y recepción. Consultas no son obligatorias de auditar.

## 8. Contratos de API (además de los del reto anterior)

Documentados en Swagger/OpenAPI.

| Método y ruta | Comportamiento mínimo |
| --- | --- |
| `GET /kpis` | Cuatro indicadores, movimientos de ayer y `calculadoEn`. |
| `GET /productos/{id}/stock` | Stock total y desglose por bodega, desde movimientos. |
| `GET /productos/riesgo` | Productos en riesgo con proveedor, stock, consumo, punto de reorden, cobertura y bodega destino sugerida. |
| `GET /bodegas/criticas` | Bodegas con ocupación ≥ 90 %. |
| `GET /proveedores` | Proveedores precargados. |
| `GET /ordenes` | Órdenes; filtro opcional `estado`. |
| `POST /ordenes` | Crea orden en `BORRADOR`; calcula `total` en el servidor. |
| `GET /ordenes/{id}` | Una orden. |
| `POST /ordenes/{id}/pdf` | Genera y guarda el PDF. |
| `GET /ordenes/{id}/pdf` | PDF guardado. |
| `PATCH /ordenes/{id}/estado` | Cambia estado con las reglas. Cuerpo exacto: `{ "estado": "APROBADA" }` (el valor es el estado destino). |
| `POST /panel/resumen` | Valida y publica el resumen. |
| `GET /panel/resumen` | Último resumen válido o 404. |

### Ejemplo fijo de `GET /kpis`

```json
{
  "calculadoEn": "2026-08-24T06:00:00-05:00",
  "ocupacionPorBodega": [{ "bodegaId": 1, "nombre": "Bogota", "porcentaje": 92.5 }],
  "productosEnQuiebre": 1,
  "productosEnRiesgo": 2,
  "ordenesPorAprobar": { "cantidad": 1, "montoTotal": 45000.0 },
  "movimientosAyer": { "entrada": 2, "salida": 3, "transferencia": 1 }
}
```

### Campos de cada elemento en `GET /productos/riesgo`

`productoId`, `nombreProducto`, `proveedorId`, `stockTotal`, `consumoDiarioPromedio`, `puntoReorden`, `diasCobertura`, `estadoCobertura`, `bodegaDestinoId`.

## 9. JSON Schema estricto de `POST /panel/resumen`

El cuerpo acepta **solo** esta estructura (`additionalProperties: false` en raíz y en ítems).

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://logitrack.local/schemas/panel-resumen.json",
  "title": "PanelResumen",
  "type": "object",
  "additionalProperties": false,
  "required": ["fecha", "narrativa", "alertas", "accionesSugeridas"],
  "properties": {
    "fecha": {
      "type": "string",
      "format": "date",
      "pattern": "^[0-9]{4}-[0-9]{2}-[0-9]{2}$",
      "description": "YYYY-MM-DD; debe ser la fecha actual en America/Bogota."
    },
    "narrativa": {
      "type": "string",
      "minLength": 20,
      "maxLength": 500
    },
    "alertas": {
      "type": "array",
      "items": { "$ref": "#/$defs/alerta" }
    },
    "accionesSugeridas": {
      "type": "array",
      "items": { "$ref": "#/$defs/accion" }
    }
  },
  "$defs": {
    "alerta": {
      "type": "object",
      "additionalProperties": false,
      "required": ["severidad", "titulo", "detalle", "productoId", "ordenId", "bodegaId"],
      "properties": {
        "severidad": { "type": "string", "enum": ["BAJA", "MEDIA", "ALTA"] },
        "titulo": { "type": "string" },
        "detalle": { "type": "string" },
        "productoId": { "type": ["integer", "null"] },
        "ordenId": { "type": ["integer", "null"] },
        "bodegaId": { "type": ["integer", "null"] }
      },
      "description": "Debe enlazar al menos un identificador no nulo; cada ID informado debe existir."
    },
    "accion": {
      "type": "object",
      "additionalProperties": false,
      "required": ["tipo", "descripcion", "ordenId", "productoId", "bodegaId"],
      "properties": {
        "tipo": {
          "type": "string",
          "enum": ["REVISAR_ORDEN", "REVISAR_PRODUCTO", "REVISAR_BODEGA"]
        },
        "descripcion": { "type": "string" },
        "ordenId": { "type": ["integer", "null"] },
        "productoId": { "type": ["integer", "null"] },
        "bodegaId": { "type": ["integer", "null"] }
      },
      "description": "Debe enlazar exactamente un identificador no nulo; ese ID debe existir."
    }
  }
}
```

Ejemplo válido (del reto):

```json
{
  "fecha": "2026-08-24",
  "narrativa": "Hay productos en riesgo y una orden pendiente de aprobación.",
  "alertas": [
    {
      "severidad": "ALTA",
      "titulo": "Producto en riesgo",
      "detalle": "Producto X está por debajo de su punto de reorden.",
      "productoId": 12,
      "ordenId": null,
      "bodegaId": 3
    }
  ],
  "accionesSugeridas": [
    {
      "tipo": "REVISAR_ORDEN",
      "descripcion": "Revisar la orden 14 antes de aprobarla.",
      "ordenId": 14,
      "productoId": null,
      "bodegaId": null
    }
  ]
}
```

La implementación **puede** validar con DTOs, Bean Validation y servicio; no se exige una librería de JSON Schema.

## 10. MCP, skill y n8n (comportamiento esperado)

MCP: exactamente seis herramientas que llaman a la API REST con usuario `AGENTE`. No accede a MySQL ni implementa reglas de negocio. **No** existe herramienta para aprobar órdenes.

1. `consultar_stock_producto(productoId)` → `GET /productos/{id}/stock`
2. `consultar_bodegas_criticas()` → `GET /bodegas/criticas`
3. `consultar_productos_en_riesgo()` → `GET /productos/riesgo`
4. `consultar_kpis()` → `GET /kpis`
5. `crear_orden_borrador(productoId, proveedorId, bodegaDestinoId, cantidad, precioUnitario)` → `POST /ordenes`
6. `publicar_resumen(resumen)` → `POST /panel/resumen`

Flujo n8n **Resumen diario de inventario**:

1. Schedule Trigger a las **6:00 a. m.** en `America/Bogota`.
2. Nodo AI Agent con herramientas MCP y las reglas de la skill.
3. Consulta KPIs y productos en riesgo.
4. Si hay productos en riesgo, crea **como máximo una** orden para el **primer** producto listado. Cantidad: `ceil(max(1, puntoReorden × 2 - stockTotal))`.
5. Publica el resumen del panel.
6. Registra salida de éxito o error en la ejecución de n8n.

Skill (`skills/operacion-logitrack/SKILL.md`), como mínimo: consultar primero riesgos y KPIs; máximo una orden en borrador por ejecución; no aprobar, cancelar ni recibir; publicar solo JSON del contrato; informar el error si una herramienta falla.

## 11. Dashboard (`frontend/`)

HTML, CSS y JavaScript sin framework. Debe:

- mostrar los cuatro indicadores, movimientos de ayer y ocupación por bodega;
- mostrar narrativa, alertas y acciones del último resumen;
- mostrar productos en riesgo y órdenes en `BORRADOR`;
- permitir generar y visualizar el PDF de una orden en `BORRADOR` (marca de agua);
- reutilizar el login JWT; guardar el JWT **solo** en `sessionStorage`;
- mostrar el botón **Aprobar** solo a un `ADMIN` autenticado;
- actualizar la tabla después de aprobar una orden.

## 12. Pruebas mínimas (escribirse antes de implementar las reglas nuevas)

1. Consumo 0: cobertura `null` y estado `SIN_CONSUMO`.
2. Stock igual al punto de reorden: no está en riesgo.
3. Cantidad 0 o negativa: `400`.
4. Orden cancelada: no se puede aprobar (`400`).
5. Orden aprobada recibida: genera una entrada.
6. `AGENTE` intenta aprobar: `403`.
7. Resumen con severidad inválida o ID inexistente: `400` y se conserva el resumen anterior.
8. PDF de orden en `BORRADOR`: se guarda y contiene la marca de agua; al cambiar el estado, ya no queda disponible hasta generarlo de nuevo.

Al menos una prueba de integración para `PATCH /ordenes/{id}/estado` o `POST /panel/resumen`.

---

## 13. Mapa de implementación (estado actual del código)

Esta sección enlaza cada bloque de la especificación con su ubicación en el repositorio. Detalle ampliado: [`../alineacion-requisitos-pdf.md`](../alineacion-requisitos-pdf.md) y [`../arquitectura-codigo.md`](../arquitectura-codigo.md).

| Bloque especificación | Servicio / componente principal | Archivo |
| --- | --- | --- |
| Reglas inventario y KPIs | `IndicadoresInventarioService` | `logitrack/.../service/IndicadoresInventarioService.java` |
| Productos en riesgo / stock | idem + `ProductoController` | `controller/ProductoController.java` |
| Bodegas críticas | idem + `BodegaController` | `controller/BodegaController.java` |
| Órdenes y transiciones | `OrdenCompraService` | `service/OrdenCompraService.java` |
| PDF orden | `OrdenPdfService` | `service/OrdenPdfService.java` |
| Panel resumen | `PanelResumenService` | `service/PanelResumenService.java` |
| Seguridad AGENTE/ADMIN | `SecurityConfig` | `security/SecurityConfig.java` |
| Dashboard (consumo API) | `loadDashboard`, `renderKpis`, etc. | `static/app.js` |
| MCP 6 tools | FastMCP | `mcp-server/main.py` |
| Flujo n8n | Workflow exportado | `n8n/resumen-diario-inventario.json` |
| Datos reproducibles | SQL | `logitrack/database/schema.sql`, `data.sql` |

### Pruebas (paquete `iq` y unitarias)

| Regla §12 | Clase de prueba |
| --- | --- |
| 1–2 | `IndicadoresInventarioServiceTest` |
| 3–6, 8–9 | `OrdenCompraEstadoTest` |
| 7, 9 | `PanelResumenTest` |
| 8 (unitario PDF) | `OrdenPdfServiceTest` |

Trazabilidad completa: [`evidencia-sdd.md`](evidencia-sdd.md).
