package com.example.logitrack.controller;

import com.example.logitrack.model.Usuario;
import com.example.logitrack.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerUsuarioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<Usuario> crearUsuario(@RequestBody Usuario usuario, Authentication authentication) {
        String adminUsername = authentication != null ? authentication.getName() : "SYSTEM";
        Usuario creado = usuarioService.crearUsuario(usuario, adminUsername);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizarUsuario(@PathVariable Long id,
                                                       @RequestBody Usuario usuario,
                                                       Authentication authentication) {
        String adminUsername = authentication != null ? authentication.getName() : "SYSTEM";
        Usuario actualizado = usuarioService.actualizarUsuario(id, usuario, adminUsername);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id, Authentication authentication) {
        String adminUsername = authentication != null ? authentication.getName() : "SYSTEM";
        usuarioService.eliminarUsuario(id, adminUsername);
        return ResponseEntity.noContent().build();
    }
}
