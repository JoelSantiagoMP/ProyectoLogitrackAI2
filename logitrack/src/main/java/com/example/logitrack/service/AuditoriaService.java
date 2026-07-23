package com.example.logitrack.service;

import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.Auditoria;
import com.example.logitrack.model.TipoOperacion;
import com.example.logitrack.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional
    public Auditoria registrarAuditoria(TipoOperacion tipoOperacion, String usuario, String entidadAfectada,
                                         Long entidadId, String valorAnterior, String valorNuevo) {
        Auditoria auditoria = Auditoria.builder()
                .tipoOperacion(tipoOperacion)
                .fechaHora(LocalDateTime.now())
                .usuario(usuario != null ? usuario : "SYSTEM")
                .entidadAfectada(entidadAfectada)
                .entidadId(entidadId)
                .valorAnterior(valorAnterior)
                .valorNuevo(valorNuevo)
                .build();
        return auditoriaRepository.save(auditoria);
    }

    @Transactional(readOnly = true)
    public List<Auditoria> obtenerTodas() {
        return auditoriaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Auditoria obtenerPorId(Long id) {
        return auditoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoría no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Auditoria> obtenerPorUsuario(String usuario) {
        return auditoriaRepository.findByUsuarioIgnoreCase(usuario);
    }

    @Transactional(readOnly = true)
    public List<Auditoria> obtenerPorTipoOperacion(TipoOperacion tipoOperacion) {
        return auditoriaRepository.findByTipoOperacion(tipoOperacion);
    }

    @Transactional(readOnly = true)
    public List<Auditoria> obtenerPorEntidad(String entidadAfectada) {
        return auditoriaRepository.findByEntidadAfectadaIgnoreCase(entidadAfectada);
    }
}
