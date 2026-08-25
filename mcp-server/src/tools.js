import { z } from "zod";
import * as api from "./api-client.js";

function ok(data) {
  return {
    content: [{ type: "text", text: typeof data === "string" ? data : JSON.stringify(data, null, 2) }],
  };
}

function fail(error) {
  return {
    isError: true,
    content: [{ type: "text", text: error instanceof Error ? error.message : String(error) }],
  };
}

const resumenSchema = z.object({
  fecha: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
  narrativa: z.string().min(20).max(500),
  alertas: z.array(z.object({
    severidad: z.enum(["BAJA", "MEDIA", "ALTA"]),
    titulo: z.string(),
    detalle: z.string(),
    productoId: z.number().int().nullable(),
    ordenId: z.number().int().nullable(),
    bodegaId: z.number().int().nullable(),
  })),
  accionesSugeridas: z.array(z.object({
    tipo: z.enum(["REVISAR_ORDEN", "REVISAR_PRODUCTO", "REVISAR_BODEGA"]),
    descripcion: z.string(),
    ordenId: z.number().int().nullable(),
    productoId: z.number().int().nullable(),
    bodegaId: z.number().int().nullable(),
  })),
});

/**
 * Registra exactamente 6 herramientas. No hay aprobar / cancelar / recibir.
 */
export function registerLogitrackTools(server) {
  server.tool(
    "consultar_stock_producto",
    "Consulta el stock total y el desglose por bodega de un producto (GET /api/productos/{id}/stock).",
    { productoId: z.number().int().positive().describe("ID del producto") },
    async ({ productoId }) => {
      try {
        return ok(await api.get(`/api/productos/${productoId}/stock`));
      } catch (error) {
        return fail(error);
      }
    },
  );

  server.tool(
    "consultar_bodegas_criticas",
    "Lista bodegas con ocupación crítica (GET /api/bodegas/criticas).",
    {},
    async () => {
      try {
        return ok(await api.get("/api/bodegas/criticas"));
      } catch (error) {
        return fail(error);
      }
    },
  );

  server.tool(
    "consultar_productos_en_riesgo",
    "Lista productos en riesgo de quiebre (GET /api/productos/riesgo).",
    {},
    async () => {
      try {
        return ok(await api.get("/api/productos/riesgo"));
      } catch (error) {
        return fail(error);
      }
    },
  );

  server.tool(
    "consultar_kpis",
    "Obtiene los KPIs de inventario (GET /api/kpis).",
    {},
    async () => {
      try {
        return ok(await api.get("/api/kpis"));
      } catch (error) {
        return fail(error);
      }
    },
  );

  server.tool(
    "crear_orden_borrador",
    "Crea una orden de compra en estado BORRADOR (POST /api/ordenes). No aprueba, no cancela y no recibe.",
    {
      productoId: z.number().int().positive(),
      proveedorId: z.number().int().positive(),
      bodegaDestinoId: z.number().int().positive(),
      cantidad: z.number().int().positive(),
      precioUnitario: z.number().positive(),
    },
    async (args) => {
      try {
        return ok(await api.post("/api/ordenes", args));
      } catch (error) {
        return fail(error);
      }
    },
  );

  server.tool(
    "publicar_resumen",
    "Publica el resumen del panel con el contrato JSON estricto (POST /api/panel/resumen).",
    { resumen: resumenSchema.describe("Cuerpo exacto de POST /panel/resumen") },
    async ({ resumen }) => {
      try {
        return ok(await api.post("/api/panel/resumen", resumen));
      } catch (error) {
        return fail(error);
      }
    },
  );
}
