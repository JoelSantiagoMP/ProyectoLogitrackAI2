package com.example.logitrack.service;

import com.example.logitrack.dto.ResultadoCobertura;
import org.springframework.stereotype.Service;

/**
 * Esqueleto TDD: las reglas de cobertura y riesgo se implementan en el ciclo verde.
 */
@Service
public class IndicadoresInventarioService {

    public ResultadoCobertura calcularCobertura(int stockTotal, double consumoDiarioPromedio) {
        throw new UnsupportedOperationException("Pendiente: cobertura y estado SIN_CONSUMO");
    }

    public boolean estaEnRiesgo(int stockTotal, double puntoReorden, boolean tieneProveedorPrincipal) {
        throw new UnsupportedOperationException("Pendiente: stock menor (no igual) al punto de reorden");
    }
}
