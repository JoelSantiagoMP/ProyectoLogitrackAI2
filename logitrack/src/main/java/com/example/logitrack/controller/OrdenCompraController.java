package com.example.logitrack.controller;

import com.example.logitrack.dto.CambioEstadoOrdenRequest;
import com.example.logitrack.dto.OrdenCompraRequest;
import com.example.logitrack.model.EstadoOrdenCompra;
import com.example.logitrack.model.OrdenCompra;
import com.example.logitrack.service.OrdenCompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/ordenes", "/ordenes"})
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    public OrdenCompraController(OrdenCompraService ordenCompraService) {
        this.ordenCompraService = ordenCompraService;
    }

    @GetMapping
    public ResponseEntity<List<OrdenCompra>> listar(@RequestParam(required = false) EstadoOrdenCompra estado) {
        return ResponseEntity.ok(ordenCompraService.listar(estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompra> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ordenCompraService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<OrdenCompra> crear(@Valid @RequestBody OrdenCompraRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        return new ResponseEntity<>(ordenCompraService.crear(request, username), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generarPdf(@PathVariable Long id) {
        return pdfResponse(id, ordenCompraService.generarPdf(id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> obtenerPdf(@PathVariable Long id) {
        return pdfResponse(id, ordenCompraService.obtenerPdf(id));
    }

    private ResponseEntity<byte[]> pdfResponse(Long id, byte[] pdf) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"orden-" + id + ".pdf\"")
                .body(pdf);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<OrdenCompra> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambioEstadoOrdenRequest request,
            Authentication authentication) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        return ResponseEntity.ok(ordenCompraService.cambiarEstado(id, request, authentication.getName(), esAdmin));
    }
}
