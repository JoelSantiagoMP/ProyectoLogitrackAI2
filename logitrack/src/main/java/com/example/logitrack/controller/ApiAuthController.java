package com.example.logitrack.controller;

import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final UsuarioRepository usuarioRepository;

    public ApiAuthController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> obtenerUsuarioActual(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null || username.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        return ResponseEntity.ok(Map.of(
                "username", usuario.getUsername(),
                "rol", usuario.getRol().name()));
    }
}
