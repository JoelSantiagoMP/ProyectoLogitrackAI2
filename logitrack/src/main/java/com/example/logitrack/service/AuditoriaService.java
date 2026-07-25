package com.example.logitrack.service;

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
                                        String entidadAfectada, String valorAnterior, String valorNuevo) {
        Auditoria auditoria = Auditoria.builder()
                .tipoOperacion(tipoOperacion)
                .fechaHora(LocalDateTime.now())
                .usuarioResponsable(usuarioResponsable)
                .entidadAfectada(entidadAfectada)
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
        // Se busca a través de la relación de la entidad Usuario (su atributo username)
        return auditoriaRepository.findByUsuarioResponsableUsernameIgnoreCase(usuario);
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