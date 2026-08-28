package com.example.logitrack.controller;

import com.example.logitrack.dto.JwtAuthResponse;
import com.example.logitrack.dto.LoginRequest;
import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.UsuarioRepository;
import com.example.logitrack.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
            UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginDto) {
        try {
            // 1. Validamos que el usuario y la contraseña sean correctos
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUsername(),
                            loginDto.getPassword()));

            // 2. Establecemos la seguridad en el contexto actual
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 3. Fabricamos el token JWT
            String token = tokenProvider.generarToken(authentication);

            Usuario usuario = usuarioRepository.findByUsername(loginDto.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Usuario no encontrado: " + loginDto.getUsername()));

            return ResponseEntity.ok(new JwtAuthResponse(token, usuario.getUsername(), usuario.getRol().name()));

        } catch (Exception e) {
            // Imprime la traza completa del error en la consola de tu IDE
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al generar token: " + e.getMessage()));
        }
    }
}