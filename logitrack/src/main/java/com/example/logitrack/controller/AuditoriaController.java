package com.example.logitrack.controller;

import com.example.logitrack.model.Auditoria;
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
    public ResponseEntity<List<Auditoria>> listarAuditorias() {
        return ResponseEntity.ok(auditoriaService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Auditoria> obtenerAuditoriaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(auditoriaService.obtenerPorId(id));
    }

    @GetMapping("/usuario/{usuario}")
    public ResponseEntity<List<Auditoria>> obtenerPorUsuario(@PathVariable String usuario) {
        // En AuditoriaService y Repository debes implementar: findByUsuarioResponsableUsername(usuario)
        return ResponseEntity.ok(auditoriaService.obtenerPorUsuario(usuario));
    }

    @GetMapping("/operacion/{tipoOperacion}")
    public ResponseEntity<List<Auditoria>> obtenerPorTipoOperacion(@PathVariable TipoOperacion tipoOperacion) {
        return ResponseEntity.ok(auditoriaService.obtenerPorTipoOperacion(tipoOperacion));
    }

    @GetMapping("/entidad/{entidadAfectada}")
    public ResponseEntity<List<Auditoria>> obtenerPorEntidad(@PathVariable String entidadAfectada) {
        return ResponseEntity.ok(auditoriaService.obtenerPorEntidad(entidadAfectada));
    }
}