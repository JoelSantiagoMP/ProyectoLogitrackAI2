package com.example.logitrack.controller;

import com.example.logitrack.config.IqOpenApiDocs;
import com.example.logitrack.dto.JwtAuthResponse;
import com.example.logitrack.dto.LoginRequest;
import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.UsuarioRepository;
import com.example.logitrack.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = IqOpenApiDocs.TAG_AUTH)
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

    @Operation(summary = "Iniciar sesión (JWT)",
            description = """
                    Autentica al usuario y devuelve un token JWT. Use el token en **Authorize** → Bearer \
                    para probar endpoints protegidos.

                    Usuarios de prueba: `admin_logitrack`, `agente_mcp`, `empleado_1` (contraseña `123456`).""")
    @ApiResponse(responseCode = "200", description = "Token JWT emitido",
            content = @Content(schema = @Schema(implementation = JwtAuthResponse.class)))
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginDto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUsername(),
                            loginDto.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = tokenProvider.generarToken(authentication);

            Usuario usuario = usuarioRepository.findByUsername(loginDto.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Usuario no encontrado: " + loginDto.getUsername()));

            return ResponseEntity.ok(new JwtAuthResponse(token, usuario.getUsername(), usuario.getRol().name()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas: " + e.getMessage()));
        }
    }
}
