package com.example.logitrack.controller;

import com.example.logitrack.config.IqOpenApiDocs;
import com.example.logitrack.dto.ProductoRequest;
import com.example.logitrack.dto.ProductoRiesgoResponse;
import com.example.logitrack.dto.StockProductoResponse;
import com.example.logitrack.model.Producto;
import com.example.logitrack.service.IndicadoresInventarioService;
import com.example.logitrack.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/productos", "/productos"})
@Tag(name = "Productos")
@SecurityRequirement(name = "bearerAuth")
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

    @Operation(summary = "Listar productos en riesgo (IQ)",
            description = """
                    Productos con proveedor principal cuyo stock total es **menor** al punto de reorden. \
                    Incluye consumo, cobertura, bodega destino sugerida.

                    """ + IqOpenApiDocs.ROLE_ADMIN_AGENTE,
            tags = {IqOpenApiDocs.TAG_KPIS})
    @IqOpenApiDocs.SecuredAdminOrAgente
    @GetMapping("/riesgo")
    public ResponseEntity<List<ProductoRiesgoResponse>> listarProductosEnRiesgo() {
        return ResponseEntity.ok(indicadoresInventarioService.listarProductosEnRiesgo());
    }

    @Operation(summary = "Stock total y desglose por bodega (IQ)",
            description = "Calculado desde movimientos. " + IqOpenApiDocs.ROLE_ADMIN_AGENTE,
            tags = {IqOpenApiDocs.TAG_KPIS})
    @IqOpenApiDocs.SecuredAdminOrAgente
    @GetMapping("/{id}/stock")
    public ResponseEntity<StockProductoResponse> obtenerStockProducto(@PathVariable Long id) {
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