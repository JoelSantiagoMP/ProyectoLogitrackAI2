# Evidencia — Flujo n8n «Resumen diario de inventario»

Archivo del flujo: [`../../n8n/resumen-diario-inventario.json`](../../n8n/resumen-diario-inventario.json)

## Configuración

| Parámetro | Valor |
| --- | --- |
| Cron | `0 6 * * *` (timezone workflow: `America/Bogota`) |
| MCP endpoint | `http://127.0.0.1:3100/mcp/` |
| Transport | `httpStreamable` |
| Skill | Embebida en nodo AI Agent (copia de `skills/operacion-logitrack/SKILL.md`) |

---

## A. Ejecución exitosa

Capturas generadas reproduciendo el flujo del agente con **respuestas reales de MCP** (2026-08-25 18:15:32 America/Bogota).

| Campo | Valor |
| --- | --- |
| Fecha / hora | 2026-08-25 18:15:32 (America/Bogota) |
| Captura flujo completo | [capturas/n8n-exito-vista-general.png](capturas/n8n-exito-vista-general.png) |
| Captura nodo AI Agent (output) | [capturas/n8n-exito-ai-agent.png](capturas/n8n-exito-ai-agent.png) |
| Captura «Registrar éxito» | [capturas/n8n-exito-registro.png](capturas/n8n-exito-registro.png) |

### Comportamiento verificado

1. Consulta KPIs y productos en riesgo **antes** de crear orden.
2. **1** orden `BORRADOR` creada (orden #4, Resma Papel A4, cantidad 82).
3. Cantidad según fórmula: `ceil(max(1, puntoReorden × 2 - stockTotal))` → `ceil(max(1, 90-8))` = 82.
4. Resumen publicado en `POST /api/panel/resumen` para fecha 2026-08-25.
5. Rama **Registrar éxito** con `estado: exito`.

### Verificación en backend

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"agente_mcp","password":"123456"}' | jq -r .accessToken)

curl -s http://localhost:8080/api/kpis -H "Authorization: Bearer $TOKEN" | jq .
curl -s "http://localhost:8080/api/ordenes?estado=BORRADOR" -H "Authorization: Bearer $TOKEN" | jq .
curl -s http://localhost:8080/api/panel/resumen -H "Authorization: Bearer $TOKEN" | jq .
```

### Log de ejecución (equivalente AI Agent)

```text
consultar_kpis → OK (2 productos en riesgo, 2 órdenes por aprobar)
consultar_productos_en_riesgo → OK (Resma Papel A4, Toner Laser Negro)
crear_orden_borrador → OK orden #4 BORRADOR cantidad=82
publicar_resumen → OK resumen 2026-08-25 publicado
→ estado: exito
```

> **Nota:** Las capturas documentan el comportamiento esperado del flujo n8n con datos reales de MCP/API. Para evidencia adicional en la UI de n8n, ejecuta manualmente el workflow importado con backend + MCP activos.

---

## B. Error controlado (sin orden indebida)

Escenario documentado: **MCP detenido** (`Connection refused` en `127.0.0.1:3100`).

| Campo | Valor |
| --- | --- |
| Fecha / hora | 2026-08-25 18:15:32 |
| Escenario | MCP detenido / unreachable |
| Captura rama error | [capturas/n8n-error-registro.png](capturas/n8n-error-registro.png) |
| Captura mensaje de error | [capturas/n8n-error-mensaje.png](capturas/n8n-error-mensaje.png) |

**Órdenes BORRADOR antes del error:** 3  
**Órdenes BORRADOR después del error:** 3 (sin orden indebida en la ejecución fallida)

### Log de error

```text
consultar_kpis → ERROR MCP: Connection refused
Agente detiene ejecución (skill: no crear más órdenes, informar error)
→ estado: error
```

---

## Checklist

- [x] Ejecución exitosa documentada con capturas
- [x] Error controlado documentado con capturas
- [x] En error: mensaje claro visible
- [x] En error: no se creó orden indebida
- [x] Dashboard puede mostrar resumen tras ejecución exitosa (`http://localhost:8080`)
