# Evidencia operativa — LogiTrack IQ

Plantillas y guías para completar los entregables que requieren capturas o logs reales (criterios 2, 3 y 4 de la rúbrica).

## Requisitos previos

1. Backend Spring Boot en `http://localhost:8080` (`cd logitrack && ./mvnw spring-boot:run`).
2. Datos cargados (`logitrack/database/schema_supabase.sql` o `schema.sql` + `data.sql`).
3. MCP en Streamable HTTP (`MCP_TRANSPORT=streamable-http`, puerto `3100`).
4. Usuario AGENTE: `agente_mcp` / `123456`.

## Contenido de esta carpeta

| Archivo / carpeta | Propósito |
| --- | --- |
| [mcp-tools.md](mcp-tools.md) | Entrada/salida esperada de las 6 tools MCP |
| [n8n-ejecucion.md](n8n-ejecucion.md) | Plantilla para ejecución exitosa y error controlado |
| [pdf-borrador.md](pdf-borrador.md) | Evidencia del PDF con marca de agua |
| `capturas/` | Guardar aquí PNG/JPG (gitignore opcional si son pesadas) |

## Cómo adjuntar evidencia

1. Ejecuta cada comando o flujo.
2. Guarda captura en `capturas/` con nombre descriptivo, por ejemplo:
   - `capturas/mcp-consultar-kpis.png`
   - `capturas/n8n-exito-2026-08-25.png`
   - `capturas/n8n-error-mcp-caido.png`
   - `capturas/pdf-borrador-watermark.png`
3. Enlaza la ruta relativa en la plantilla correspondiente (`mcp-tools.md`, `n8n-ejecucion.md`, etc.).
4. Opcional: pega logs en bloques ` ```text ` dentro de esas plantillas.

## Checklist rápido

- [ ] 6 tools MCP documentadas (entrada + respuesta o captura)
- [ ] n8n ejecución **exitosa** (KPIs + riesgo + resumen publicado)
- [ ] n8n **error controlado** (sin orden indebida; p. ej. MCP caído)
- [ ] PDF BORRADOR con watermark visible
- [ ] Video 4–6 min (externo al repo; enlace opcional abajo)

### Enlace al video (completar)

```text
URL: ________________________________
Duración: ___ min
```
