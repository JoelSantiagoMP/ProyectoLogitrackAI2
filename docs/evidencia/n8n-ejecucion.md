# Evidencia — Flujo n8n «Resumen diario de inventario»

Archivo del flujo: [`../../n8n/resumen-diario-inventario.json`](../../n8n/resumen-diario-inventario.json)

## Configuración

| Parámetro | Valor |
| --- | --- |
| Cron | `0 6 * * *` (timezone workflow: `America/Bogota`) |
| MCP endpoint | `http://127.0.0.1:3100/mcp/` (Docker: `http://host.docker.internal:3100/mcp/`) |
| Transport | `httpStreamable` |
| Skill | Embebida en nodo AI Agent (copia de `skills/operacion-logitrack/SKILL.md`) |

## A. Ejecución exitosa

Completar tras **Execute workflow** manual con backend + MCP activos.

| Campo | Valor |
| --- | --- |
| Fecha / hora | `_YYYY-MM-DD HH:MM (America/Bogota)_` |
| Captura flujo completo | `capturas/n8n-exito-vista-general.png` |
| Captura nodo AI Agent (output) | `capturas/n8n-exito-ai-agent.png` |
| Captura «Registrar éxito» | `capturas/n8n-exito-registro.png` |

### Comportamiento esperado

1. Consulta KPIs y productos en riesgo **antes** de crear orden.
2. Si hay riesgo: **como máximo 1** orden `BORRADOR` (primer producto de la lista).
3. Cantidad: `ceil(max(1, puntoReorden × 2 - stockTotal))`.
4. Publica resumen válido en `POST /api/panel/resumen`.
5. Rama **Registrar éxito** con `estado: exito`.

### Verificación en backend (opcional)

```bash
# Token AGENTE
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"agente_mcp","password":"123456"}' | jq -r .accessToken)

curl -s http://localhost:8080/api/kpis -H "Authorization: Bearer $TOKEN" | jq .
curl -s http://localhost:8080/api/ordenes?estado=BORRADOR -H "Authorization: Bearer $TOKEN" | jq .
curl -s http://localhost:8080/api/panel/resumen -H "Authorization: Bearer $TOKEN" | jq .
```

### Log de ejecución n8n

```text
_pegar salida del nodo AI Agent o del nodo Registrar éxito_
```

---

## B. Error controlado (sin orden indebida)

Objetivo rúbrica: demostrar manejo de error **sin** crear una orden que no debía existir.

### Escenario recomendado: MCP caído

1. **Detener** el servidor MCP (`Ctrl+C` en terminal `mcp-server`).
2. Ejecutar el workflow manualmente en n8n.
3. Debe caer en rama **Registrar error** (`estado: error`).
4. **No** debe aparecer una orden BORRADOR nueva creada en esta ejecución fallida.

| Campo | Valor |
| --- | --- |
| Fecha / hora | `_YYYY-MM-DD HH:MM_` |
| Escenario | `_ej. MCP detenido / backend caído / credencial inválida_` |
| Captura rama error | `capturas/n8n-error-registro.png` |
| Captura mensaje de error | `capturas/n8n-error-mensaje.png` |

### Verificación post-error

```bash
# Confirmar que no se creó orden indebida en esta corrida
curl -s "http://localhost:8080/api/ordenes?estado=BORRADOR" \
  -H "Authorization: Bearer $TOKEN" | jq 'length'
```

**Órdenes BORRADOR antes del error:** `_N_`  
**Órdenes BORRADOR después del error:** `_N_` (debe ser igual si el fallo fue antes de crear_orden)

### Log de error

```text
_pegar mensaje de error del agente o de MCP_
```

---

## Checklist

- [ ] Ejecución exitosa documentada con capturas
- [ ] Error controlado documentado con capturas
- [ ] En error: mensaje claro visible en n8n
- [ ] En error: no se creó orden indebida
- [ ] Dashboard muestra resumen tras ejecución exitosa (`http://localhost:8080`)
