package com.example.logitrack.controller;

import com.example.logitrack.dto.PanelResumenRequest;
import com.example.logitrack.model.ResumenPanel;
import com.example.logitrack.service.PanelResumenService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/panel", "/panel"})
public class PanelResumenController {

    private final PanelResumenService panelResumenService;

    public PanelResumenController(PanelResumenService panelResumenService) {
        this.panelResumenService = panelResumenService;
    }

    @PostMapping("/resumen")
    public ResponseEntity<ResumenPanel> publicar(@Valid @RequestBody PanelResumenRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(panelResumenService.publicar(request, authentication.getName()));
    }

    @GetMapping(value = "/resumen", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> obtenerUltimo() {
        ResumenPanel resumen = panelResumenService.obtenerUltimo();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resumen.getContenidoJson());
    }
}
