package com.example.logitrack.controller;

import com.example.logitrack.config.IqOpenApiDocs;
import com.example.logitrack.dto.ProveedorDTO;
import com.example.logitrack.model.Proveedor;
import com.example.logitrack.service.ProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = IqOpenApiDocs.TAG_PROVEEDORES)
@SecurityRequirement(name = "bearerAuth")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @Operation(summary = "Listar proveedores precargados",
            description = """
                    Proveedores disponibles para órdenes de compra. Campo `diasEntrega` (1–90) \
                    interviene en el punto de reorden.

                    """ + IqOpenApiDocs.ROLE_AUTHENTICATED)
    @IqOpenApiDocs.SecuredAdminOrAgente
    @GetMapping
    public ResponseEntity<List<Proveedor>> listar() {
        return ResponseEntity.ok(proveedorService.listarTodos());
    }

    @Operation(summary = "Crear proveedor", description = IqOpenApiDocs.ROLE_ADMIN, hidden = true)
    @PostMapping
    public ResponseEntity<Proveedor> crear(@Valid @RequestBody ProveedorDTO dto, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        Proveedor creado = proveedorService.crearProveedor(dto, username);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }
}
