package com.example.logitrack.examen.controller; 

import com.example.logitrack.examen.service.ReporteService; 
import com.example.logitrack.model.Auditoria; 
import com.example.logitrack.model.Movimiento; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/movimientos")
    public ResponseEntity<List<Movimiento>> getReporteMovimientos(
            @RequestParam(required = false) String bodega,
            @RequestParam(required = false) String producto,
            @RequestParam(required = false) String tipoMovimiento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        
        List<Movimiento> reporte = reporteService.obtenerReporteMovimientos(bodega, producto, tipoMovimiento, fechaInicio, fechaFin);
        return ResponseEntity.ok(reporte);
    }

    @GetMapping("/auditoria")
    public ResponseEntity<List<Auditoria>> getReporteAuditoria(
            @RequestParam(required = false) String entidadAfectada,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        
        List<Auditoria> reporte = reporteService.obtenerAuditoriasFiltradas(entidadAfectada, fechaInicio, fechaFin);
        return ResponseEntity.ok(reporte);
    }
}
