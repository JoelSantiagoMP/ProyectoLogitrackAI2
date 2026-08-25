package com.example.logitrack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
public class AlertaPanelRequest {

    public enum Severidad {
        BAJA, MEDIA, ALTA
    }

    @NotNull(message = "La severidad es obligatoria")
    private Severidad severidad;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    @NotBlank(message = "El detalle es obligatorio")
    private String detalle;

    private Long productoId;
    private Long ordenId;
    private Long bodegaId;

    public Severidad getSeveridad() {
        return severidad;
    }

    public void setSeveridad(Severidad severidad) {
        this.severidad = severidad;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Long getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(Long ordenId) {
        this.ordenId = ordenId;
    }

    public Long getBodegaId() {
        return bodegaId;
    }

    public void setBodegaId(Long bodegaId) {
        this.bodegaId = bodegaId;
    }
}
