package com.example.logitrack.service;

import com.example.logitrack.dto.AuditoriaDTO;
import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.Auditoria;
import com.example.logitrack.model.TipoOperacion;
import com.example.logitrack.model.Usuario;
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
    public Auditoria registrarAuditoria(TipoOperacion tipoOperacion, Usuario usuarioResponsable,
                                        String entidadAfectada, Long entidadId,
                                        String valorAnterior, String valorNuevo) {
        Auditoria auditoria = Auditoria.builder()
                .tipoOperacion(tipoOperacion)
                .fechaHora(LocalDateTime.now())
                .usuarioResponsable(usuarioResponsable)
                .entidadAfectada(entidadAfectada)
                .entidadId(entidadId)
                .valorAnterior(valorAnterior)
                .valorNuevo(valorNuevo)
                .build();
        return auditoriaRepository.save(auditoria);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> obtenerTodas() {
        return auditoriaRepository.findAllWithUsuario().stream().map(AuditoriaDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public AuditoriaDTO obtenerPorId(Long id) {
        Auditoria auditoria = auditoriaRepository.findByIdWithUsuario(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoría no encontrado con id: " + id));
        return AuditoriaDTO.from(auditoria);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> obtenerPorUsuario(String usuario) {
        return auditoriaRepository.findByUsuarioResponsableUsernameIgnoreCase(usuario.toLowerCase()).stream()
                .map(AuditoriaDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> obtenerPorTipoOperacion(TipoOperacion tipoOperacion) {
        return auditoriaRepository.findByTipoOperacion(tipoOperacion).stream()
                .map(AuditoriaDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> obtenerPorEntidad(String entidadAfectada) {
        return auditoriaRepository.findByEntidadAfectadaIgnoreCase(entidadAfectada.toLowerCase()).stream()
                .map(AuditoriaDTO::from)
                .toList();
    }
}
