package com.example.logitrack.controller;

import com.example.logitrack.model.DetalleMovimiento;
import com.example.logitrack.service.DetalleMovimientoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/detalles-movimiento")
public class DetalleMovimientoController {

    private final DetalleMovimientoService detalleMovimientoService;

    public DetalleMovimientoController(DetalleMovimientoService detalleMovimientoService) {
        this.detalleMovimientoService = detalleMovimientoService;
    }

    @GetMapping
    public ResponseEntity<List<DetalleMovimiento>> listarDetalles() {
        return ResponseEntity.ok(detalleMovimientoService.obtenerTodos());
    }

    @GetMapping("/movimiento/{movimientoId}")
    public ResponseEntity<List<DetalleMovimiento>> obtenerDetallesPorMovimientoId(@PathVariable Long movimientoId) {
        return ResponseEntity.ok(detalleMovimientoService.obtenerPorMovimientoId(movimientoId));
    }

    @GetMapping("/mas-movidos")
    public ResponseEntity<List<Map<String, Object>>> obtenerProductosMasMovidos() {
        List<Object[]> resultadosRaw = detalleMovimientoService.obtenerProductosMasMovidosRaw();
        List<Map<String, Object>> resultadoFormatted = new ArrayList<>();

        for (Object[] fila : resultadosRaw) {
            Map<String, Object> map = new HashMap<>();
            map.put("productoId", fila[0]);
            map.put("nombreProducto", fila[1]);
            map.put("totalCantidad", fila[2]);
            resultadoFormatted.add(map);
        }

        return ResponseEntity.ok(resultadoFormatted);
    }
}
