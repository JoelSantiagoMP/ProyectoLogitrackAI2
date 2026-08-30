package com.example.logitrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Conteo de movimientos del día calendario anterior (America/Bogota)")
public class MovimientosAyerResponse {

    @Schema(example = "2")
    private long entrada;

    @Schema(example = "3")
    private long salida;

    @Schema(example = "1")
    private long transferencia;

    @Schema(description = "Fecha calendario de referencia (día anterior)", example = "2026-08-23")
    private String fechaReferencia;

    public long getEntrada() {
        return entrada;
    }

    public void setEntrada(long entrada) {
        this.entrada = entrada;
    }

    public long getSalida() {
        return salida;
    }

    public void setSalida(long salida) {
        this.salida = salida;
    }

    public long getTransferencia() {
        return transferencia;
    }

    public void setTransferencia(long transferencia) {
        this.transferencia = transferencia;
    }

    public String getFechaReferencia() {
        return fechaReferencia;
    }

    public void setFechaReferencia(String fechaReferencia) {
        this.fechaReferencia = fechaReferencia;
    }
}
