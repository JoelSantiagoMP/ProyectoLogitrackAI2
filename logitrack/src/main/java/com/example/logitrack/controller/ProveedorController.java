package com.example.logitrack.controller;

import com.example.logitrack.dto.ProveedorDTO;
import com.example.logitrack.model.Proveedor;
import com.example.logitrack.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/proveedores", "/proveedores"})
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public ResponseEntity<List<Proveedor>> listar() {
        return ResponseEntity.ok(proveedorService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<Proveedor> crear(@Valid @RequestBody ProveedorDTO dto, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        Proveedor creado = proveedorService.crearProveedor(dto, username);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }
}
