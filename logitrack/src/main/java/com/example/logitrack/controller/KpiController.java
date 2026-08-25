package com.example.logitrack.controller;

import com.example.logitrack.service.IndicadoresInventarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class KpiController {

    private final IndicadoresInventarioService indicadoresInventarioService;

    public KpiController(IndicadoresInventarioService indicadoresInventarioService) {
        this.indicadoresInventarioService = indicadoresInventarioService;
    }

    @GetMapping({"/api/kpis", "/kpis"})
    public ResponseEntity<Map<String, Object>> obtenerKpis() {
        return ResponseEntity.ok(indicadoresInventarioService.obtenerKpis());
    }
}
