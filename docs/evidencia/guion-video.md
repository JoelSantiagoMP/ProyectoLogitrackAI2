# Guía para grabar el video (4–6 min)

Checklist operativo para demostrar el flujo completo **sin mostrar código**, con presentación fluida. El reto califica funcionamiento y narración del producto, no archivos fuente.

---

## ¿Está todo listo?

| Área | Estado | Notas |
| --- | --- | --- |
| Backend + API IQ | ✅ | KPIs, riesgo, órdenes, PDF, panel |
| Dashboard | ✅ | KPIs, resumen n8n, riesgo, BORRADOR, Aprobar/Recibir (ADMIN) |
| MCP (6 tools) | ✅ | Evidencia en [`evidencia/mcp-tools.md`](evidencia/mcp-tools.md) |
| Skill + flujo n8n | ✅ | `n8n/resumen-diario-inventario.json` |
| Swagger documentado | ✅ | http://localhost:8080/swagger-ui.html |
| SDD + alineación PDF | ✅ | [`docs/README.md`](../README.md) |
| Capturas MCP/n8n/PDF | ✅ | [`evidencia/capturas/`](capturas/) |
| **Video grabado** | ⬜ | Único entregable que falta |

**Conclusión:** el sistema está listo para demostrarse. Solo falta la grabación y, antes de ella, un **ensayo en seco** (15–20 min).

---

## Antes de grabar — pre-vuelo (3 terminales)

Ejecuta **en este orden** y no cierres las ventanas durante el video:

### Terminal 1 — Backend

```bash
cd logitrack
export DB_URL="jdbc:postgresql://HOST:5432/postgres?currentSchema=logitrack"
export DB_USERNAME="tu_usuario"
export DB_PASSWORD="tu_password"
./mvnw spring-boot:run
```

Verifica: http://localhost:8080 carga el login.

### Terminal 2 — MCP (obligatorio antes de n8n)

```bash
cd mcp-server
source .venv/bin/activate
export MCP_TRANSPORT=streamable-http
export MCP_HOST=0.0.0.0
export MCP_PORT=3100
python main.py
```

Verifica: logs `Uvicorn running` y respuestas `200 OK` en `/mcp`.

### Terminal 3 — n8n

- Importa `n8n/resumen-diario-inventario.json` si aún no lo hiciste.
- El workflow puede estar `active: false`; para el video usas **Execute workflow** manual.
- Endpoint MCP en n8n: `http://127.0.0.1:3100/mcp/` (o `host.docker.internal` si n8n está en Docker).

---

## Estado limpio recomendado (opcional pero ayuda)

Para que el video se vea claro (pocos BORRADOR viejos, números coherentes):

1. Anota KPIs actuales en dashboard (`admin_logitrack` / `123456`).
2. Si hay muchas órdenes BORRADOR de pruebas, puedes cancelarlas como ADMIN o dejar **una sola** pendiente antes de grabar.
3. Ejecuta n8n **una vez en ensayo** y confirma que crea **como máximo 1** orden nueva + publica resumen.

---

## Disposición de ventanas (presentación bonita)

Usa **pantalla completa por escena** o ventanas grandes; evita mezclar IDE/código.

| Escena | Qué mostrar | Ventana |
| --- | --- | --- |
| 1. Contexto | Dashboard login o KPIs con productos en riesgo | Navegador `:8080` |
| 2. Automatización | n8n → Execute workflow → nodo AI Agent → éxito | n8n (pestaña dedicada) |
| 3. Torre de control | Dashboard: resumen del panel + tabla riesgo + BORRADOR nueva | Navegador |
| 4. Decisión humana | Aprobar orden (ADMIN) | Navegador |
| 5. Recepción | Recibir → KPIs/stock actualizados | Navegador |
| 6. (Opcional) | Swagger Authorize + GET `/api/kpis` o PDF BORRADOR con watermark | Navegador |
| 7. (Opcional 5 s) | Diagrama arquitectura en `docs/diagrama-arquitectura.md` renderizado en GitHub/preview | Solo si cabe en 6 min |

**No mostrar:** Cursor, archivos `.java`, `.py`, terminal con código, `.env` con contraseñas.

**Sí mostrar:** n8n, Swagger UI, dashboard, PDF con marca de agua **BORRADOR** (pestaña del navegador).

---

## Guion minuto a minuto (~5 min)

### 0:00 – 0:45 · Problema y contexto

**Pantalla:** Dashboard (sin login aún o vista general tras login).

**Narración sugerida:**

> «LogiTrack ya tenía inventario en backend, pero no había una torre de control diaria. Esta extensión detecta productos en riesgo, propone una compra en borrador y deja al administrador aprobar y recibir la mercancía.»

**Mostrar:** tarjetas KPI (productos en riesgo, órdenes por aprobar).

---

### 0:45 – 2:00 · n8n + MCP (automatización)

**Pantalla:** n8n — workflow *Resumen diario de inventario*.

**Acciones:**

1. Mostrar Schedule 06:00 America/Bogota (no hace falta esperar al cron).
2. Clic **Execute workflow**.
3. Abrir nodo **AI Agent** → output: KPIs, productos en riesgo.
4. Mostrar rama **Registrar éxito** (`estado: exito`).

**Narración:**

> «Cada mañana el agente consulta la API vía MCP: KPIs y productos en riesgo. Si hay riesgo, crea como máximo una orden en BORRADOR y publica el resumen del panel. El agente no puede aprobar órdenes; eso queda en manos del administrador.»

**No mencionar** nombres de archivos ni código.

---

### 2:00 – 3:15 · Dashboard — resumen y orden BORRADOR

**Pantalla:** http://localhost:8080 — login `admin_logitrack` / `123456`.

**Mostrar:**

- Bloque **Resumen del panel** (narrativa, alertas, acciones de n8n).
- Tabla **Productos en riesgo**.
- Tabla **Órdenes en BORRADOR** (la creada por n8n).
- **Generar PDF** → **Ver** → marca de agua diagonal **BORRADOR**.

**Narración:**

> «El administrador ve el mismo resumen que publicó el flujo, la tabla de riesgo y la orden propuesta. El PDF en borrador lleva marca de agua hasta que se aprueba.»

---

### 3:15 – 4:30 · Aprobar, recibir, inventario actualizado

**Acciones:**

1. Clic **Aprobar** en la orden BORRADOR.
2. Clic **Recibir** (orden APROBADA).
3. Refrescar / observar KPIs (productos en riesgo u órdenes por aprobar cambian).
4. (Opcional) Página Productos o Movimientos — movimiento **ENTRADA** reciente.

**Narración:**

> «Al aprobar, la orden pasa a revisión humana. Al recibirla, el backend registra automáticamente una entrada de inventario en la bodega destino. El dashboard refleja el inventario actualizado: el faltante detectado por el sistema se cerró con una compra controlada.»

---

### 4:30 – 5:30 · Cierre (Swagger o arquitectura, opcional)

**Opción A — Swagger:** Authorize con JWT → `GET /api/kpis` → esquema con indicadores.

**Opción B — Diagrama:** `docs/diagrama-arquitectura.md` (n8n → MCP → API → BD → dashboard).

**Narración cierre:**

> «La API es la única fuente de verdad; n8n y MCP solo consultan y proponen. El flujo completo queda probado: riesgo, borrador, aprobación, recepción y stock actualizado.»

---

## Credenciales (tenlas a mano en nota, no en pantalla)

| Usuario | Rol | Contraseña | Uso en video |
| --- | --- | --- | --- |
| `admin_logitrack` | ADMIN | `123456` | Dashboard: aprobar, recibir, PDF |
| `agente_mcp` | AGENTE | `123456` | Solo si muestras Swagger como agente |

---

## Errores comunes y cómo evitarlos

| Problema | Solución |
| --- | --- |
| n8n falla con Connection refused | Levanta MCP **antes** de ejecutar n8n |
| Dashboard sin resumen | Ejecuta n8n exitoso primero o publica resumen manual |
| PDF 404 tras aprobar | Normal: explica que hay que regenerar PDF tras cambio de estado |
| Aparece código en grabación | Cierra IDE; usa solo navegador + n8n |
| Video > 6 min | Corta opcional Swagger; mantén flujo n8n → dashboard → aprobar → recibir |
| Muchas órdenes BORRADOR | Ensaya con BD limpia o cancela extras antes de grabar |

---

## Ensayo en seco (15 min, el día anterior)

1. [ ] Backend `:8080` OK  
2. [ ] MCP `:3100` OK  
3. [ ] n8n Execute → éxito  
4. [ ] Dashboard muestra resumen + BORRADOR  
5. [ ] Aprobar + Recibir + KPIs cambian  
6. [ ] PDF BORRADOR con watermark visible  
7. [ ] Cronometrar: objetivo 4–6 min  

---

## Después de grabar

1. Sube el video (YouTube/Drive) y pega la URL en [`evidencia/README.md`](README.md) sección «Enlace al video».
2. Marca checklist sección F en [`sdd/04-tareas.md`](../sdd/04-tareas.md).

---

## Enlaces rápidos

- [Evidencia MCP](mcp-tools.md)
- [Evidencia n8n](n8n-ejecucion.md)
- [PDF BORRADOR](pdf-borrador.md)
- [Wireframes](../wireframes.md)
- [Alineación PDF](../alineacion-requisitos-pdf.md)
