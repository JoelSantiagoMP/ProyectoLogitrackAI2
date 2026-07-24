
package com.example.logitrack.controller;

import com.example.logitrack.dto.JwtAuthResponse;
import com.example.logitrack.dto.LoginRequest;
import com.example.logitrack.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> authenticateUser(@RequestBody LoginRequest loginDto) {
        
        // 1. Validamos que el usuario y la contraseña sean correctos
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()
                )
        );

        // 2. Establecemos la seguridad en el contexto actual
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Fabricamos el token JWT
        String token = tokenProvider.generarToken(authentication);

        // 4. Se lo enviamos al usuario en formato JSON
        return ResponseEntity.ok(new JwtAuthResponse(token));
    }
}