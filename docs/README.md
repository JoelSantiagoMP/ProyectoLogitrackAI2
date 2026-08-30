# Documentación — LogiTrack IQ

Índice central de toda la documentación del proyecto **Proyecto integrador IA2 — LogiTrack IQ: Torre de control de inventario**. Cada documento describe el estado **actual** del repositorio (agosto 2026) y enlaza con el código, las pruebas y las evidencias operativas.

## Mapa de documentos

| Documento | Contenido | Audiencia |
| --- | --- | --- |
| [README raíz](../README.md) | Instalación, usuarios, inicio rápido | Cualquiera que clone el repo |
| [Alineación con el PDF del reto](alineacion-requisitos-pdf.md) | Requisito por requisito: cumplimiento, justificación y ubicación en código | Evaluación / revisión de rúbrica |
| [Arquitectura (diagrama)](diagrama-arquitectura.md) | n8n → MCP → API → BD → dashboard | Visión general del sistema |
| [Arquitectura en código](arquitectura-codigo.md) | Paquetes, servicios, controladores, repositorios | Desarrolladores |
| [Dashboard y responsive](frontend-dashboard.md) | UI, wireframes, breakpoints, consumo de API | Frontend / demostración |
| [Wireframes](wireframes.md) | Bocetos ASCII y Mermaid de pantallas IQ | Diseño / video |
| [SDD 01 — Propuesta](sdd/01-propuesta.md) | Problema, objetivo, alcance | Proceso SDD |
| [SDD 02 — Especificación](sdd/02-especificacion.md) | Reglas, contratos API, pruebas mínimas | Contrato técnico |
| [SDD 03 — Diseño](sdd/03-diseno.md) | Entidades, decisiones, flujo | Diseño |
| [SDD 04 — Tareas](sdd/04-tareas.md) | Checklist de entregables | Seguimiento |
| [Evidencia SDD/TDD](sdd/evidencia-sdd.md) | Commits, regla → prueba, rojo/verde | Certificación TDD |
| [Guía para grabar el video](evidencia/guion-video.md) | Guion, ventanas y pre-vuelo | Demostración / video |
| [Evidencia operativa (MCP, n8n, PDF)](evidencia/README.md) | Capturas y logs | Demostración funcional |
| [logitrack/README.md](../logitrack/README.md) | Backend, endpoints, estructura Java | Backend |
| [mcp-server/README.md](../mcp-server/README.md) | 6 tools, variables, transporte HTTP | Integración MCP |
| [skills/operacion-logitrack/SKILL.md](../skills/operacion-logitrack/SKILL.md) | Reglas del agente n8n | Automatización |

## Estructura real del repositorio

El PDF de referencia sugiere `frontend/` en la raíz. En este proyecto el dashboard vive en **`logitrack/src/main/resources/static/`** porque Spring Boot sirve la SPA en el mismo puerto que la API (sin CORS adicional en desarrollo). La carpeta [`frontend/`](../frontend/README.md) en la raíz es un **índice documental** que explica esa decisión; el comportamiento exigido por el reto está implementado.

```
ProyectoLogitrackAI2-1/
├── docs/                    ← Estás aquí
├── logitrack/               ← Backend Spring Boot + dashboard (static/)
├── mcp-server/              ← Servidor MCP Python
├── n8n/                     ← Flujo exportado
├── skills/                  ← Skill operacion-logitrack
└── frontend/                ← Puntero documental (no duplica código)
```

## Base de datos

El enunciado del reto menciona MySQL como convención genérica (“no modificar MySQL directamente”). La implementación usa **PostgreSQL** (incl. Supabase) con esquema `logitrack`. Los scripts reproducibles están en `logitrack/database/schema.sql` y `data.sql`. El comportamiento de negocio (stock desde movimientos, transacciones, auditoría) es equivalente al exigido.

## Flujo de demostración (objetivo del reto)

1. **Datos iniciales** — `data.sql` deja productos en riesgo y movimientos `ENTRADA`.
2. **n8n + MCP** — El flujo *Resumen diario de inventario* consulta KPIs/riesgo y crea **como máximo una** orden `BORRADOR`.
3. **ADMIN** — En el dashboard, `admin_logitrack` aprueba la orden (`PATCH` estado `APROBADA`).
4. **Recepción** — El ADMIN recibe la orden (`RECIBIDA`); el backend crea un movimiento `ENTRADA` en la misma transacción.
5. **Dashboard** — KPIs, tablas y stock reflejan el inventario actualizado.

## Pruebas automatizadas

```bash
cd logitrack && ./mvnw test
```

Trazabilidad regla → prueba: [evidencia-sdd.md](sdd/evidencia-sdd.md).

## Pendiente externo al código

- Video de 4–6 minutos (sin mostrar código): ver checklist en [04-tareas.md](sdd/04-tareas.md) sección F.
