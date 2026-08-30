package com.example.logitrack.dto;

import com.example.logitrack.model.EstadoOrdenCompra;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Cuerpo de PATCH /api/ordenes/{id}/estado. Ejemplo: { \"estado\": \"APROBADA\" }")
public class CambioEstadoOrdenRequest {

    @Schema(description = "Estado destino", allowableValues = {"BORRADOR", "APROBADA", "RECIBIDA", "CANCELADA"},
            example = "APROBADA")
    @NotNull(message = "El estado es obligatorio")
    private EstadoOrdenCompra estado;

    public EstadoOrdenCompra getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrdenCompra estado) {
        this.estado = estado;
    }
}
