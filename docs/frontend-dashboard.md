# Dashboard web y diseño responsive

Documentación del frontend de LogiTrack IQ: ubicación real, consumo de API, comportamiento por rol y verificación responsive.

## Ubicación del código

El PDF del reto pide una carpeta `frontend/` con HTML, CSS y JavaScript sin framework. En este repositorio el dashboard está en:

| Archivo | Ruta |
| --- | --- |
| HTML | `logitrack/src/main/resources/static/index.html` |
| CSS | `logitrack/src/main/resources/static/style.css` |
| JavaScript | `logitrack/src/main/resources/static/app.js` |

**Justificación:** Spring Boot sirve archivos estáticos en la raíz (`http://localhost:8080`). La SPA y la API comparten origen, lo que simplifica el envío del JWT sin configurar CORS en desarrollo. El enunciado permite adaptar la estructura al proyecto anterior siempre que las responsabilidades estén separadas; la separación lógica está en `app.js` (sección DASHBOARD IQ) y en la documentación.

Ver también: [`frontend/README.md`](../frontend/README.md) y [`wireframes.md`](wireframes.md).

---

## Requisitos del PDF cubiertos por el dashboard

| Requisito | Cómo se cumple |
| --- | --- |
| Cuatro indicadores | Tarjetas `#kpi-ocupacion`, `#kpi-quiebre`, `#kpi-riesgo`, `#kpi-ordenes-cant` |
| Ocupación por bodega | Lista en `#kpi-ocupacion-list` con barras y % |
| Movimientos de ayer | Chips `#ayer-entrada`, `#ayer-salida`, `#ayer-transferencia` |
| Resumen n8n | `#panel-resumen-body` (narrativa, alertas, acciones) |
| Productos en riesgo | Tabla `#tbody-riesgo` |
| Órdenes BORRADOR | Tabla `#tbody-ordenes-borrador` |
| PDF BORRADOR con watermark | Botones Generar/Ver → endpoints PDF |
| JWT en `sessionStorage` | `saveSession` / `clearSession` |
| Aprobar solo ADMIN | `botonesEstado()` retorna `—` si no `isAdmin()` |
| Actualizar tras aprobar | `cambiarEstadoOrden` → `loadDashboard()` |

**Extra (no exigido pero útil):** tarjeta de órdenes `APROBADA` pendientes de recepción para completar el flujo ADMIN sin salir del dashboard (`#card-ordenes-aprobada`).

---

## Carga del dashboard — secuencia de API

Función principal: `loadDashboard()` en `app.js`.

```javascript
// Peticiones en paralelo (Promise.allSettled)
apiFetch('/api/kpis')
apiFetchOptional('/api/panel/resumen')  // 404 si n8n aún no publicó
apiFetch('/api/productos/riesgo')
apiFetch('/api/ordenes?estado=BORRADOR')
apiFetch('/api/bodegas')                // cache para nombres de bodega sugerida
// Solo ADMIN:
apiFetch('/api/ordenes?estado=APROBADA')
```

**Motivo de `allSettled`:** Si el resumen del panel no existe (404), el resto del dashboard sigue cargando. El KPI y la tabla de riesgo no dependen del flujo n8n.

**Motivo de no calcular en cliente:** Todos los números (stock, punto de reorden, ocupación) vienen del backend; el frontend solo renderiza (convención “fuente de verdad” del PDF).

---

## Autenticación y roles

| Clave `sessionStorage` | Contenido |
| --- | --- |
| `logitrack_token` | JWT Bearer |
| `logitrack_user` | Nombre de usuario |
| `logitrack_rol` | `ADMIN`, `AGENTE` o `EMPLEADO` |

`apiFetch` añade `Authorization: Bearer <token>` en cada petición.

| Rol | Dashboard IQ |
| --- | --- |
| **ADMIN** | Ve todo; botones Aprobar, Recibir, Cancelar; sección órdenes APROBADAS |
| **AGENTE** | Ve KPIs, riesgo, BORRADOR, PDF; **sin** botones de cambio de estado |
| **EMPLEADO** | Acceso al sistema legacy; sin privilegios IQ de aprobación |

La visibilidad de elementos `.admin-only` se ajusta en `applyRoleVisibility()` al iniciar sesión.

---

## Acciones de órdenes y PDF

| Acción UI | Método | Ruta | Rol |
| --- | --- | --- | --- |
| Aprobar | PATCH | `/api/ordenes/{id}/estado` `{ "estado": "APROBADA" }` | ADMIN |
| Recibir | PATCH | `{ "estado": "RECIBIDA" }` | ADMIN |
| Cancelar | PATCH | `{ "estado": "CANCELADA" }` | ADMIN |
| Generar PDF | POST | `/api/ordenes/{id}/pdf` | ADMIN, AGENTE |
| Ver PDF | GET | `/api/ordenes/{id}/pdf` | ADMIN, AGENTE |

Tras cambiar estado, el PDF previo deja de existir (404 en GET hasta regenerar). Evidencia: `docs/evidencia/capturas/pdf-404-tras-aprobar.png`.

---

## Diseño responsive — verificación por dispositivo

El reto **no califica** interfaz móvil avanzada, pero sí **legibilidad**. Se implementaron tres breakpoints en `style.css`:

### Breakpoint 1024px (tablet horizontal / laptop pequeña)

```css
@media (max-width: 1024px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
```

| Dispositivo típico | Comportamiento esperado |
| --- | --- |
| iPad landscape (~1024px) | 4 KPI en cuadrícula 2×2 |
| Laptop 13" | Sidebar visible; contenido usable |

### Breakpoint 768px (tablet vertical / móvil grande)

```css
@media (max-width: 768px) {
  .sidebar { transform: translateX(-100%); }
  .sidebar.open { transform: translateX(0); }
  .menu-toggle { display: flex; }
  .main-content { margin-left: 0; }
  .dashboard-grid { grid-template-columns: 1fr; }
  .form-row, .filter-group { grid-template-columns: 1fr; }
  .table-wrapper { overflow-x: auto; }  /* scroll horizontal tablas */
}
```

| Dispositivo típico | Comportamiento esperado |
| --- | --- |
| iPad portrait (~768px) | Menú hamburguesa; sidebar como drawer con overlay |
| iPhone Plus / Android grande | KPI 2×2; tablas con scroll horizontal |
| Formularios (bodegas, productos) | Una columna |

**Controles móviles:** `#menu-toggle`, `#sidebar-close`, `#sidebar-overlay` en `index.html` + listeners en `app.js`.

### Breakpoint 480px (móvil pequeño)

```css
@media (max-width: 480px) {
  .stats-grid { grid-template-columns: 1fr; }
  .login-card { padding: 1.75rem 1.25rem; }
}
```

| Dispositivo típico | Comportamiento esperado |
| --- | --- |
| iPhone SE (~375px) | KPI en columna única |
| Login | Tarjeta más compacta, sin desbordamiento |

### Viewport

```html
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
```

Presente en `index.html` línea 6.

### Checklist de verificación manual

1. **Desktop (≥1280px):** sidebar fija, 4 KPI en fila, tablas completas visibles.
2. **1024px:** KPI 2×2; sin solapamiento de tarjetas.
3. **768px:** abrir menú ☰; cerrar con overlay o ✕; contenido ocupa ancho completo.
4. **480px:** KPI apilados; login legible; tablas desplazables horizontalmente sin romper la página.
5. **Funcional:** con `admin_logitrack`, botón Aprobar visible; con `agente_mcp`, columna Acciones muestra `—`.

### Cómo probar en el navegador

1. `cd logitrack && ./mvnw spring-boot:run`
2. Abrir `http://localhost:8080`
3. DevTools → Toggle device toolbar (Chrome) o Responsive Design Mode (Firefox)
4. Probar anchos: 1280, 1024, 768, 390 px

---

## Estilos IQ específicos

Clases en `style.css` (sección IQ, ~líneas 1254–1309):

| Clase | Uso |
| --- | --- |
| `.iq-meta` | Texto “Calculado en … America/Bogota” |
| `.ocupacion-row`, `.ocupacion-bar` | Barras de ocupación; `.critica` si ≥90% |
| `.movimientos-ayer`, `.ayer-chip` | Contadores de ayer |
| `.alerta-card`, `.sev-ALTA` | Alertas del resumen por severidad |
| `.acciones-list` | Acciones sugeridas del panel |
| `.badge-estado-*` | Estados de orden en tabla |

---

## Páginas adicionales (reto anterior)

El mismo `index.html` incluye bodegas, productos, movimientos, reportes, auditoría y usuarios. Comparten el layout responsive y el patrón `apiFetch`. No son parte de la rúbrica IQ pero demuestran que el backend anterior sigue operativo.

---

## Enlaces

- Wireframes: [`wireframes.md`](wireframes.md)
- Alineación PDF: [`alineacion-requisitos-pdf.md`](alineacion-requisitos-pdf.md)
- Evidencia PDF: [`evidencia/pdf-borrador.md`](evidencia/pdf-borrador.md)
