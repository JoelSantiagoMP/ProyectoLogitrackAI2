package com.example.logitrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Indicadores de la torre de control IQ (GET /api/kpis)")
public class KpisResponse {

    @Schema(description = "Marca de tiempo del cálculo en America/Bogota", example = "2026-08-24T06:00:00-05:00")
    private OffsetDateTime calculadoEn;

    @Schema(description = "Ocupación por bodega: (unidades almacenadas / capacidad) × 100")
    private List<OcupacionBodegaResponse> ocupacionPorBodega = new ArrayList<>();

    @Schema(description = "Cantidad de productos cuyo stock total es 0", example = "1")
    private int productosEnQuiebre;

    @Schema(description = "Productos con proveedor principal y stock total menor al punto de reorden", example = "2")
    private int productosEnRiesgo;

    @Schema(description = "Órdenes en estado BORRADOR: cantidad y suma de totales")
    private OrdenesPorAprobarResponse ordenesPorAprobar;

    @Schema(description = "Conteo de movimientos del día calendario anterior en America/Bogota")
    private MovimientosAyerResponse movimientosAyer;

    public OffsetDateTime getCalculadoEn() {
        return calculadoEn;
    }

    public void setCalculadoEn(OffsetDateTime calculadoEn) {
        this.calculadoEn = calculadoEn;
    }

    public List<OcupacionBodegaResponse> getOcupacionPorBodega() {
        return ocupacionPorBodega;
    }

    public void setOcupacionPorBodega(List<OcupacionBodegaResponse> ocupacionPorBodega) {
        this.ocupacionPorBodega = ocupacionPorBodega;
    }

    public int getProductosEnQuiebre() {
        return productosEnQuiebre;
    }

    public void setProductosEnQuiebre(int productosEnQuiebre) {
        this.productosEnQuiebre = productosEnQuiebre;
    }

    public int getProductosEnRiesgo() {
        return productosEnRiesgo;
    }

    public void setProductosEnRiesgo(int productosEnRiesgo) {
        this.productosEnRiesgo = productosEnRiesgo;
    }

    public OrdenesPorAprobarResponse getOrdenesPorAprobar() {
        return ordenesPorAprobar;
    }

    public void setOrdenesPorAprobar(OrdenesPorAprobarResponse ordenesPorAprobar) {
        this.ordenesPorAprobar = ordenesPorAprobar;
    }

    public MovimientosAyerResponse getMovimientosAyer() {
        return movimientosAyer;
    }

    public void setMovimientosAyer(MovimientosAyerResponse movimientosAyer) {
        this.movimientosAyer = movimientosAyer;
    }
}
