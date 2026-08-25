package com.example.logitrack.service;

import com.example.logitrack.dto.BodegaDTO;
import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.Bodega;
import com.example.logitrack.model.TipoOperacion;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.BodegaRepository;
import com.example.logitrack.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BodegaService {

    private final BodegaRepository bodegaRepository;
    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;

    public BodegaService(BodegaRepository bodegaRepository,
            AuditoriaService auditoriaService,
            UsuarioRepository usuarioRepository) {
        this.bodegaRepository = bodegaRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Bodega> obtenerTodas() {
        return bodegaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Bodega obtenerPorId(Long id) {
        return bodegaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada con id: " + id));
    }

    @Transactional(readOnly = true)
    public Bodega obtenerPorNombre(String nombre) {
        return bodegaRepository.findByNombre(nombre)
                .orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada con nombre: " + nombre));
    }

    @Transactional
    public Bodega crearBodega(BodegaDTO dto, String username) {
        if (bodegaRepository.existsByNombre(dto.getNombre())) {
            throw new IllegalArgumentException("Ya existe una bodega con el nombre: " + dto.getNombre());
        }

        Usuario usuarioResponsable = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario responsable no encontrado: " + username));

        Bodega bodega = new Bodega();
        bodega.setNombre(dto.getNombre());
        bodega.setUbicacion(dto.getUbicacion());
        bodega.setCapacidad(dto.getCapacidad());

        if (dto.getEncargadoId() != null) {
            Usuario encargadoReal = usuarioRepository.findById(dto.getEncargadoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "El usuario encargado no existe con ID: " + dto.getEncargadoId()));
            bodega.setEncargado(encargadoReal);
        } else {
            bodega.setEncargado(usuarioResponsable);
        }

        Bodega guardada = bodegaRepository.save(bodega);

        auditoriaService.registrarAuditoria(
                TipoOperacion.INSERT,
                usuarioResponsable,
                "Bodega",
                guardada.getId(),
                null,
                guardada.getNombre() + " (Ubicación: " + guardada.getUbicacion() + ", Capacidad: "
                        + guardada.getCapacidad() + ")");

        return guardada;
    }

    @Transactional
    public Bodega actualizarBodega(Long id, BodegaDTO dto, String username) {
        Bodega bodegaExistente = obtenerPorId(id);

        Usuario usuarioResponsable = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario responsable no encontrado: " + username));

        String valorAnterior = bodegaExistente.getNombre() + " (Ubicación: " + bodegaExistente.getUbicacion()
                + ", Capacidad: " + bodegaExistente.getCapacidad() + ")";

        bodegaExistente.setNombre(dto.getNombre());
        bodegaExistente.setUbicacion(dto.getUbicacion());
        bodegaExistente.setCapacidad(dto.getCapacidad());

        if (dto.getEncargadoId() != null) {
            Usuario encargadoReal = usuarioRepository.findById(dto.getEncargadoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "El usuario encargado no existe con ID: " + dto.getEncargadoId()));
            bodegaExistente.setEncargado(encargadoReal);
        }

        Bodega guardada = bodegaRepository.save(bodegaExistente);
        String valorNuevo = guardada.getNombre() + " (Ubicación: " + guardada.getUbicacion() + ", Capacidad: "
                + guardada.getCapacidad() + ")";

        auditoriaService.registrarAuditoria(
                TipoOperacion.UPDATE,
                usuarioResponsable,
                "Bodega",
                guardada.getId(),
                valorAnterior,
                valorNuevo);

        return guardada;
    }

    @Transactional
    public void eliminarBodega(Long id, String username) {
        Bodega bodega = obtenerPorId(id);

        Usuario usuarioResponsable = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario responsable no encontrado: " + username));

        String valorAnterior = bodega.getNombre() + " (Encargado ID: "
                + (bodega.getEncargado() != null ? bodega.getEncargado().getId() : "N/A") + ")";

        bodegaRepository.delete(bodega);

        auditoriaService.registrarAuditoria(
                TipoOperacion.DELETE,
                usuarioResponsable,
                "Bodega",
                bodega.getId(),
                valorAnterior,
                null);
    }
}