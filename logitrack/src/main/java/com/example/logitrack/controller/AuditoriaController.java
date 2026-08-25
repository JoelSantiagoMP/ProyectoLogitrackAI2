package com.example.logitrack.controller;

import com.example.logitrack.dto.AuditoriaDTO;
import com.example.logitrack.model.TipoOperacion;
import com.example.logitrack.service.AuditoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<List<AuditoriaDTO>> listarAuditorias() {
        return ResponseEntity.ok(auditoriaService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditoriaDTO> obtenerAuditoriaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(auditoriaService.obtenerPorId(id));
    }

    @GetMapping("/usuario/{usuario}")
    public ResponseEntity<List<AuditoriaDTO>> obtenerPorUsuario(@PathVariable String usuario) {
        return ResponseEntity.ok(auditoriaService.obtenerPorUsuario(usuario));
    }

    @GetMapping("/operacion/{tipoOperacion}")
    public ResponseEntity<List<AuditoriaDTO>> obtenerPorTipoOperacion(@PathVariable TipoOperacion tipoOperacion) {
        return ResponseEntity.ok(auditoriaService.obtenerPorTipoOperacion(tipoOperacion));
    }

    @GetMapping("/entidad/{entidadAfectada}")
    public ResponseEntity<List<AuditoriaDTO>> obtenerPorEntidad(@PathVariable String entidadAfectada) {
        return ResponseEntity.ok(auditoriaService.obtenerPorEntidad(entidadAfectada));
    }
}
