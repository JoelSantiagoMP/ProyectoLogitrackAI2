package com.example.logitrack.controller;

import com.example.logitrack.dto.AuditoriaDTO;
import com.example.logitrack.dto.InventarioReporteDTO;
import com.example.logitrack.model.Movimiento;
import com.example.logitrack.model.TipoMovimiento;
import com.example.logitrack.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/movimientos")
    public ResponseEntity<List<Movimiento>> getReporteMovimientos(
            @RequestParam(required = false) String bodega,
            @RequestParam(required = false) String producto,
            @RequestParam(required = false) TipoMovimiento tipoMovimiento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        return ResponseEntity.ok(reporteService.obtenerReporteMovimientos(
                bodega, producto, tipoMovimiento, fechaInicio, fechaFin));
    }

    @GetMapping("/auditoria")
    public ResponseEntity<List<AuditoriaDTO>> getReporteAuditoria(
            @RequestParam(required = false) String entidadAfectada,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        return ResponseEntity.ok(reporteService.obtenerAuditoriasFiltradas(entidadAfectada, fechaInicio, fechaFin));
    }

    @GetMapping("/inventario")
    public ResponseEntity<List<InventarioReporteDTO>> getReporteInventario(
            @RequestParam(required = false) Long bodegaId) {
        return ResponseEntity.ok(reporteService.obtenerReporteInventario(bodegaId));
    }
}
