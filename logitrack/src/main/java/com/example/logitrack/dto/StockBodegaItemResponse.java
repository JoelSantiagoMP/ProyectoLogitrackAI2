package com.example.logitrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stock de un producto en una bodega")
public class StockBodegaItemResponse {

    @Schema(example = "1")
    private Long bodegaId;

    @Schema(example = "Bogota")
    private String nombre;

    @Schema(example = "30")
    private int cantidad;

    public Long getBodegaId() {
        return bodegaId;
    }

    public void setBodegaId(Long bodegaId) {
        this.bodegaId = bodegaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
