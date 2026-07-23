package com.example.logitrack.controller;

import com.example.logitrack.model.Bodega;
import com.example.logitrack.service.BodegaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bodegas")
public class BodegaController {

    @Autowired
    private BodegaService bodegaService;

    // GET: http://localhost:8080/api/bodegas
    @GetMapping
    public List<Bodega> listarBodegas() {
        return bodegaService.obtenerTodas();
    }

    // GET: http://localhost:8080/api/bodegas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Bodega> obtenerBodega(@PathVariable Long id) {
        return bodegaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST: http://localhost:8080/api/bodegas
    @PostMapping
    public Bodega crearBodega(@RequestBody Bodega bodega) {
        return bodegaService.guardarBodega(bodega);
    }

    // DELETE: http://localhost:8080/api/bodegas/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarBodega(@PathVariable Long id) {
        bodegaService.eliminarBodega(id);
        return ResponseEntity.noContent().build();
    }
}
