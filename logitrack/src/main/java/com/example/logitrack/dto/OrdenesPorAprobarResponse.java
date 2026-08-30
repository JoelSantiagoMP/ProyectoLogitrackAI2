package com.example.logitrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumen de órdenes en estado BORRADOR pendientes de aprobación")
public class OrdenesPorAprobarResponse {

    @Schema(example = "1")
    private int cantidad;

    @Schema(description = "Suma de los totales de las órdenes BORRADOR", example = "45000.0")
    private double montoTotal;

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(double montoTotal) {
        this.montoTotal = montoTotal;
    }
}
