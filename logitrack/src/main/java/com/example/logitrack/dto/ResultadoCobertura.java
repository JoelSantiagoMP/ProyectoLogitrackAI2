package com.example.logitrack.dto;

public class ResultadoCobertura {

    private final Double diasCobertura;
    private final String estadoCobertura;

    public ResultadoCobertura(Double diasCobertura, String estadoCobertura) {
        this.diasCobertura = diasCobertura;
        this.estadoCobertura = estadoCobertura;
    }

    public Double getDiasCobertura() {
        return diasCobertura;
    }

    public String getEstadoCobertura() {
        return estadoCobertura;
    }
}
