package com.example.logitrack.dto;

import com.example.logitrack.model.EstadoOrdenCompra;
import jakarta.validation.constraints.NotNull;

public class CambioEstadoOrdenRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoOrdenCompra estado;

    public EstadoOrdenCompra getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrdenCompra estado) {
        this.estado = estado;
    }
}
