package com.example.logitrack.controller;

import com.example.logitrack.config.IqOpenApiDocs;
import com.example.logitrack.dto.BodegaCriticaResponse;
import com.example.logitrack.dto.BodegaDTO;
import com.example.logitrack.model.Bodega;
import com.example.logitrack.service.BodegaService;
import com.example.logitrack.service.IndicadoresInventarioService;
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
@RequestMapping({"/api/bodegas", "/bodegas"})
@Tag(name = "Bodegas")
@SecurityRequirement(name = "bearerAuth")
public class BodegaController {

    private final BodegaService bodegaService;
    private final IndicadoresInventarioService indicadoresInventarioService;

    public BodegaController(BodegaService bodegaService,
            IndicadoresInventarioService indicadoresInventarioService) {
        this.bodegaService = bodegaService;
        this.indicadoresInventarioService = indicadoresInventarioService;
    }

    @GetMapping
    public ResponseEntity<List<Bodega>> listarBodegas() {
        return ResponseEntity.ok(bodegaService.obtenerTodas());
    }

    @Operation(summary = "Bodegas con ocupación crítica (IQ)",
            description = "Lista bodegas con ocupación ≥ 90 %. " + IqOpenApiDocs.ROLE_ADMIN_AGENTE,
            tags = {IqOpenApiDocs.TAG_KPIS})
    @IqOpenApiDocs.SecuredAdminOrAgente
    @GetMapping("/criticas")
    public ResponseEntity<List<BodegaCriticaResponse>> listarBodegasCriticas() {
        return ResponseEntity.ok(indicadoresInventarioService.listarBodegasCriticas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bodega> obtenerBodegaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bodegaService.obtenerPorId(id));
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<Bodega> obtenerBodegaPorNombre(@PathVariable String nombre) {
        return ResponseEntity.ok(bodegaService.obtenerPorNombre(nombre));
    }

    @PostMapping
    public ResponseEntity<Bodega> crearBodega(@Valid @RequestBody BodegaDTO dto, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        Bodega creada = bodegaService.crearBodega(dto, username);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Bodega> actualizarBodega(@PathVariable Long id,
            @Valid @RequestBody BodegaDTO dto,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        Bodega actualizada = bodegaService.actualizarBodega(id, dto, username);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarBodega(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        bodegaService.eliminarBodega(id, username);
        return ResponseEntity.noContent().build();
    }
}