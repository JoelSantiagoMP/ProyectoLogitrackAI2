package com.example.logitrack.controller;

import com.example.logitrack.model.Movimiento;
import com.example.logitrack.model.TipoMovimiento;
import com.example.logitrack.service.MovimientoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {

    private final MovimientoService movimientoService;

    public MovimientoController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    @GetMapping
    public ResponseEntity<List<Movimiento>> listarMovimientos() {
        return ResponseEntity.ok(movimientoService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movimiento> obtenerMovimientoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(movimientoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<Movimiento> registrarMovimiento(@Valid @RequestBody Movimiento movimiento, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        Movimiento registrado = movimientoService.registrarMovimiento(movimiento, username);
        return new ResponseEntity<>(registrado, HttpStatus.CREATED);
    }

    @GetMapping("/rango-fechas")
    public ResponseEntity<List<Movimiento>> obtenerPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(movimientoService.obtenerPorRangoFechas(inicio, fin));
    }

    // Spring realiza el casting automático de String a Enum si el string coincide exactamente (ignorando mayúsculas/minúsculas depende de la config)
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<Movimiento>> obtenerPorTipo(@PathVariable TipoMovimiento tipo) {
        return ResponseEntity.ok(movimientoService.obtenerPorTipo(tipo));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Movimiento>> obtenerPorUsuarioId(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(movimientoService.obtenerPorUsuarioId(usuarioId));
    }

    @GetMapping("/bodega/{bodegaId}")
    public ResponseEntity<List<Movimiento>> obtenerPorBodegaId(@PathVariable Long bodegaId) {
        return ResponseEntity.ok(movimientoService.obtenerPorBodegaId(bodegaId));
    }
}