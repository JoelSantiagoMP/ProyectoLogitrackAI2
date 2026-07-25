package com.example.logitrack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<String> inicio() {
        return ResponseEntity.ok("¡Bienvenido a la API de LogiTrack! La aplicación está funcionando correctamente.");
    }
}
