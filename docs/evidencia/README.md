# Evidencia operativa — LogiTrack IQ

Evidencias generadas el **2026-08-25** con backend (`:8080`), MCP (`:3100`) y API reales.

Documentación general del proyecto: [`../README.md`](../README.md) · Alineación PDF: [`../alineacion-requisitos-pdf.md`](../alineacion-requisitos-pdf.md).

## Regenerar capturas

```bash
cd logitrack && ./mvnw spring-boot:run          # terminal 1
cd mcp-server && source .venv/bin/activate
MCP_TRANSPORT=streamable-http MCP_PORT=3100 python main.py   # terminal 2
cd mcp-server && python ../docs/evidencia/generar_capturas.py  # terminal 3
```

## Archivos

| Documento | Estado |
| --- | --- |
| [mcp-tools.md](mcp-tools.md) | 6/6 tools con captura |
| [n8n-ejecucion.md](n8n-ejecucion.md) | Éxito + error controlado |
| [pdf-borrador.md](pdf-borrador.md) | Watermark + 404 tras aprobar |
| [capturas-evidencia.json](capturas-evidencia.json) | Log JSON de respuestas MCP |
| [generar_capturas.py](generar_capturas.py) | Script reproducible |

## Capturas (`capturas/`)

| Archivo | Descripción |
| --- | --- |
| `mcp-consultar-*.png` | 6 tools MCP |
| `n8n-exito-*.png` | Flujo exitoso (respuestas reales) |
| `n8n-error-*.png` | Error MCP sin orden indebida |
| `pdf-borrador-watermark.png` | Marca de agua diagonal BORRADOR |
| `pdf-404-tras-aprobar.png` | PDF invalidado al cambiar estado |
| `orden-borrador-1.pdf` | PDF binario guardado |

## Checklist

- [x] 6 tools MCP documentadas (entrada + respuesta + captura)
- [x] n8n ejecución exitosa (KPIs + riesgo + resumen + 1 orden BORRADOR)
- [x] n8n error controlado (sin orden indebida)
- [x] PDF BORRADOR con watermark visible
- [ ] Video 4–6 min (externo al repo) — guía: [`guion-video.md`](guion-video.md)

### Enlace al video (completar)

```text
URL: ________________________________
Duración: ___ min
```
