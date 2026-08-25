package com.example.logitrack.service;

import com.example.logitrack.dto.ResultadoCobertura;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class IndicadoresInventarioServiceTest {

    private final IndicadoresInventarioService service = new IndicadoresInventarioService();

    @Test
    void consumoCero_coberturaNullYSinConsumo() {
        ResultadoCobertura resultado = service.calcularCobertura(40, 0.0);

        assertNull(resultado.getDiasCobertura());
        assertEquals("SIN_CONSUMO", resultado.getEstadoCobertura());
    }

    @Test
    void stockIgualPuntoReorden_noEstaEnRiesgo() {
        boolean enRiesgo = service.estaEnRiesgo(15, 15.0, true);

        assertFalse(enRiesgo);
    }
}
