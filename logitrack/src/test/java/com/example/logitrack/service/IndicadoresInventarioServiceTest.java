package com.example.logitrack.service;

import com.example.logitrack.dto.ResultadoCobertura;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void stockCeroConProveedor_estaEnRiesgoAunSinConsumo() {
        boolean enRiesgo = service.estaEnRiesgo(0, 0.0, true);

        assertTrue(enRiesgo);
    }

    @Test
    void stockCeroSinProveedor_noEstaEnRiesgo() {
        boolean enRiesgo = service.estaEnRiesgo(0, 45.0, false);

        assertFalse(enRiesgo);
    }

    @Test
    void stockPositivoSinConsumo_noEstaEnRiesgo() {
        boolean enRiesgo = service.estaEnRiesgo(10, 0.0, true);

        assertFalse(enRiesgo);
    }

    @Test
    void ocupacion_calculaPorcentajeSinTopeArtificial() {
        assertEquals(0.3, service.calcularPorcentajeOcupacion(75, 25000), 0.001);
        assertEquals(93.75, service.calcularPorcentajeOcupacion(75, 80), 0.001);
        assertEquals(125.0, service.calcularPorcentajeOcupacion(125, 100), 0.001);
    }

    @Test
    void ocupacion_capacidadInvalida_devuelveCero() {
        assertEquals(0.0, service.calcularPorcentajeOcupacion(75, 0), 0.001);
        assertEquals(0.0, service.calcularPorcentajeOcupacion(75, null), 0.001);
    }
}
