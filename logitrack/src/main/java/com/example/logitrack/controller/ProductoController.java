package com.example.logitrack.controller;

import com.example.logitrack.dto.ProductoRequest;
import com.example.logitrack.model.Producto;
import com.example.logitrack.service.IndicadoresInventarioService;
import com.example.logitrack.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final IndicadoresInventarioService indicadoresInventarioService;

    public ProductoController(ProductoService productoService,
            IndicadoresInventarioService indicadoresInventarioService) {
        this.productoService = productoService;
        this.indicadoresInventarioService = indicadoresInventarioService;
    }

    @GetMapping
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(productoService.obtenerTodos());
    }

    @GetMapping("/stock-bajo")
    public ResponseEntity<List<Producto>> obtenerProductosStockBajo() {
        return ResponseEntity.ok(productoService.obtenerProductosStockBajo());
    }

    @GetMapping("/riesgo")
    public ResponseEntity<List<Map<String, Object>>> listarProductosEnRiesgo() {
        return ResponseEntity.ok(indicadoresInventarioService.listarProductosEnRiesgo());
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> obtenerStockProducto(@PathVariable Long id) {
        return ResponseEntity.ok(indicadoresInventarioService.obtenerStockProducto(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerProductoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @GetMapping("/{id}/inventario/{bodegaId}")
    public ResponseEntity<Map<String, Integer>> obtenerStockEnBodega(@PathVariable Long id,
            @PathVariable Long bodegaId) {
        int cantidad = productoService.obtenerStockEnBodega(id, bodegaId);
        return ResponseEntity.ok(Map.of("cantidad", cantidad));
    }

    @PostMapping
    public ResponseEntity<Producto> crearProducto(@Valid @RequestBody ProductoRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        Producto creado = productoService.crearProducto(request, username);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizarProducto(@PathVariable Long id,
            @Valid @RequestBody ProductoRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        Producto actualizado = productoService.actualizarProducto(id, request, username);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        productoService.eliminarProducto(id, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<Producto>> obtenerProductosPorCategoria(@PathVariable String categoria) {
        return ResponseEntity.ok(productoService.obtenerPorCategoria(categoria));
    }
}