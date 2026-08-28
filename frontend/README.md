# Frontend — LogiTrack IQ

El enunciado del reto integrador IA2 solicita una carpeta `frontend/` en la raíz del repositorio con HTML, CSS y JavaScript sin framework.

## Ubicación real del código

El dashboard **no está duplicado** aquí. Los archivos fuente viven en:

```
logitrack/src/main/resources/static/
├── index.html    # SPA completa (login + dashboard IQ + módulos legacy)
├── style.css     # Estilos incl. responsive IQ
└── app.js        # Consumo API, JWT sessionStorage, lógica IQ
```

Se sirven automáticamente en **http://localhost:8080** cuando ejecutas `./mvnw spring-boot:run` dentro de `logitrack/`.

## Por qué no hay una carpeta `frontend/` con código

| Razón | Detalle |
| --- | --- |
| Proyecto anterior | LogiTrack ya servía la UI desde `static/` en Spring Boot |
| Mismo origen API/UI | Evita CORS y simplifica despliegue (un solo artefacto JAR) |
| Enunciado flexible | “La estructura exacta puede adaptarse al proyecto anterior, siempre que las responsabilidades estén separadas de forma clara” |
| Comportamiento idéntico | Cumple todos los requisitos funcionales del dashboard IQ del PDF |

La **responsabilidad** del frontend (presentación, sin reglas de negocio) está separada en archivos dedicados y documentada en:

- [`docs/frontend-dashboard.md`](../docs/frontend-dashboard.md)
- [`docs/wireframes.md`](../docs/wireframes.md)
- [`docs/alineacion-requisitos-pdf.md`](../docs/alineacion-requisitos-pdf.md) (sección 11)

## Si necesitas una carpeta `frontend/` física

Opciones válidas para entrega (no requeridas en este repo):

1. **Symlink:** `frontend → logitrack/src/main/resources/static`
2. **Copia en build:** script que copie `static/` a `frontend/` antes del commit de entrega
3. **Servidor estático aparte:** nginx sirviendo `frontend/` apuntando a la misma API (más complejo, innecesario para la rúbrica)

Este repositorio optó por documentar la ubicación real en lugar de duplicar archivos.

## Inicio rápido

```bash
cd logitrack
./mvnw spring-boot:run
# Abrir http://localhost:8080
# Login: admin_logitrack / 123456
```
