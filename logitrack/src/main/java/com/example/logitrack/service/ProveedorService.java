package com.example.logitrack.service;

import com.example.logitrack.dto.ProveedorDTO;
import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.Proveedor;
import com.example.logitrack.model.TipoOperacion;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.ProveedorRepository;
import com.example.logitrack.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;

    public ProveedorService(ProveedorRepository proveedorRepository,
            AuditoriaService auditoriaService,
            UsuarioRepository usuarioRepository) {
        this.proveedorRepository = proveedorRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Proveedor> listarTodos() {
        return proveedorRepository.findAll();
    }

    @Transactional
    public Proveedor crearProveedor(ProveedorDTO dto, String username) {
        String nombre = dto.getNombre() != null ? dto.getNombre().trim() : "";
        if (proveedorRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalArgumentException("Ya existe un proveedor con el nombre: " + nombre);
        }

        Usuario responsable = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario responsable no encontrado: " + username));

        Proveedor proveedor = Proveedor.builder()
                .nombre(nombre)
                .email(dto.getEmail() != null ? dto.getEmail().trim() : null)
                .diasEntrega(dto.getDiasEntrega())
                .build();

        Proveedor guardado = proveedorRepository.save(proveedor);

        auditoriaService.registrarAuditoria(
                TipoOperacion.INSERT,
                responsable,
                "Proveedor",
                guardado.getId(),
                null,
                guardado.getNombre() + " (Email: " + guardado.getEmail() + ", Días entrega: "
                        + guardado.getDiasEntrega() + ")");

        return guardado;
    }
}
