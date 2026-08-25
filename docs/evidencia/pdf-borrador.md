# Evidencia — PDF orden en BORRADOR

Requisito: PDF guardado en la orden con **marca de agua diagonal** legible `BORRADOR`.

## Pasos para generar evidencia

1. Iniciar sesión como **ADMIN** (`admin_logitrack` / `123456`) en [http://localhost:8080](http://localhost:8080).
2. Ir a **Órdenes de compra** o al dashboard (tabla órdenes BORRADOR).
3. En una orden en estado `BORRADOR`, pulsar **Generar PDF** y luego **Ver**.
4. Capturar pantalla del visor PDF mostrando la watermark.

| Campo | Valor |
| --- | --- |
| Fecha | `_YYYY-MM-DD_` |
| ID orden | `_ej. 14_` |
| Captura UI | `capturas/pdf-generar-desde-dashboard.png` |
| Captura watermark | `capturas/pdf-borrador-watermark.png` |

## Verificación por API (alternativa)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin_logitrack","password":"123456"}' | jq -r .accessToken)

# Sustituir {id} por orden BORRADOR existente
curl -s -X POST "http://localhost:8080/api/ordenes/{id}/pdf" \
  -H "Authorization: Bearer $TOKEN" \
  -o capturas/orden-borrador-{id}.pdf

# Abrir el archivo y verificar watermark diagonal "BORRADOR"
```

## Verificación tras cambio de estado

1. Aprobar la orden (ADMIN) → `PATCH .../estado` con `APROBADA`.
2. `GET /api/ordenes/{id}/pdf` debe responder **404** hasta regenerar PDF.
3. Captura opcional: `capturas/pdf-404-tras-aprobar.png`

## Checklist

- [ ] PDF generado y guardado (campo `fechaGeneracionPdf` en orden)
- [ ] Watermark diagonal `BORRADOR` visible en captura
- [ ] Tras aprobar: PDF anterior no disponible (404)
- [ ] Regenerar PDF refleja nuevo estado (sin watermark BORRADOR si APROBADA)
