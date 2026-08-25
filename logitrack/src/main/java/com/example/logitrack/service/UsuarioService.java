package com.example.logitrack.service;

import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.TipoOperacion;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    @Transactional
    public Usuario crearUsuario(Usuario usuario, String adminUsername) {
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre de usuario: " + usuario.getUsername());
        }
        
        Usuario admin = usuarioRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado: " + adminUsername));

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        Usuario guardado = usuarioRepository.save(usuario);

        auditoriaService.registrarAuditoria(
                TipoOperacion.INSERT,
                admin,
                "Usuario",
                guardado.getId(),
                null,
                "Username: " + guardado.getUsername() + " (Rol: " + guardado.getRol() + ")"
        );

        return guardado;
    }

    @Transactional
    public Usuario actualizarUsuario(Long id, Usuario usuarioActualizado, String adminUsername) {
        Usuario usuarioExistente = obtenerPorId(id);
        
        Usuario admin = usuarioRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado: " + adminUsername));
                
        String valorAnterior = "Username: " + usuarioExistente.getUsername() + " (Rol: " + usuarioExistente.getRol() + ")";

        usuarioExistente.setUsername(usuarioActualizado.getUsername());
        if (usuarioActualizado.getRol() != null) {
            usuarioExistente.setRol(usuarioActualizado.getRol());
        }
        if (usuarioActualizado.getPassword() != null && !usuarioActualizado.getPassword().isBlank()) {
            usuarioExistente.setPassword(passwordEncoder.encode(usuarioActualizado.getPassword()));
        }

        Usuario guardado = usuarioRepository.save(usuarioExistente);
        String valorNuevo = "Username: " + guardado.getUsername() + " (Rol: " + guardado.getRol() + ")";

        auditoriaService.registrarAuditoria(
                TipoOperacion.UPDATE,
                admin,
                "Usuario",
                guardado.getId(),
                valorAnterior,
                valorNuevo
        );

        return guardado;
    }

    @Transactional
    public void eliminarUsuario(Long id, String adminUsername) {
        Usuario usuario = obtenerPorId(id);
        
        Usuario admin = usuarioRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado: " + adminUsername));
                
        String valorAnterior = "Username: " + usuario.getUsername() + " (Rol: " + usuario.getRol() + ")";

        usuarioRepository.delete(usuario);

        auditoriaService.registrarAuditoria(
                TipoOperacion.DELETE,
                admin,
                "Usuario",
                usuario.getId(),
                valorAnterior,
                null
        );
    }
}