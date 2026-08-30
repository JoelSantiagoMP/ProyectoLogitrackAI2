package com.example.logitrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ocupación de una bodega en unidades y porcentaje")
public class OcupacionBodegaResponse {

    @Schema(example = "1")
    private Long bodegaId;

    @Schema(example = "Bogota")
    private String nombre;

    @Schema(description = "Porcentaje de ocupación (unidades/capacidad)×100", example = "92.5")
    private double porcentaje;

    @Schema(description = "Unidades almacenadas en la bodega", example = "740")
    private int unidades;

    @Schema(description = "Capacidad máxima en unidades de producto", example = "800")
    private Integer capacidad;

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

    public double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public int getUnidades() {
        return unidades;
    }

    public void setUnidades(int unidades) {
        this.unidades = unidades;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }
}
