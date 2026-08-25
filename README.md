# Proyecto LogiTrack AI

Aplicación **LogiTrack**: gestión de bodegas, inventario, movimientos, auditoría, reportes y **torre de control IQ** (KPIs, órdenes de compra, panel n8n/MCP).

La implementación Spring Boot está en [`logitrack/`](logitrack/). Documentación SDD en [`docs/sdd/`](docs/sdd/). Diagrama: [`docs/diagrama-arquitectura.md`](docs/diagrama-arquitectura.md).

## Estructura del repositorio

```
docs/sdd/          Documentación SDD + evidencia TDD
logitrack/         Backend Spring Boot + dashboard (static/)
mcp-server/        Servidor MCP (rol AGENTE)
n8n/               Flujo Resumen diario de inventario
skills/            Skill operacion-logitrack
```

## Inicio rápido

```bash
cd logitrack
./mvnw spring-boot:run
```

Abre [http://localhost:8080](http://localhost:8080).
