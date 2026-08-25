package com.example.logitrack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
public class AccionPanelRequest {

    public enum TipoAccion {
        REVISAR_ORDEN, REVISAR_PRODUCTO, REVISAR_BODEGA
    }

    @NotNull(message = "El tipo de acción es obligatorio")
    private TipoAccion tipo;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    private Long ordenId;
    private Long productoId;
    private Long bodegaId;

    public TipoAccion getTipo() {
        return tipo;
    }

    public void setTipo(TipoAccion tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(Long ordenId) {
        this.ordenId = ordenId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Long getBodegaId() {
        return bodegaId;
    }

    public void setBodegaId(Long bodegaId) {
        this.bodegaId = bodegaId;
    }
}
