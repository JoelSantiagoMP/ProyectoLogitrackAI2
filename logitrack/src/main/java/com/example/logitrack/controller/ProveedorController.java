package com.example.logitrack.controller;

import com.example.logitrack.model.Proveedor;
import com.example.logitrack.repository.ProveedorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProveedorController {

    private final ProveedorRepository proveedorRepository;

    public ProveedorController(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @GetMapping({"/api/proveedores", "/proveedores"})
    public ResponseEntity<List<Proveedor>> listar() {
        return ResponseEntity.ok(proveedorRepository.findAll());
    }
}
