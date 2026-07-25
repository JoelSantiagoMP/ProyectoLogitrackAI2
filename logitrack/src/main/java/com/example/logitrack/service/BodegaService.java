package com.example.logitrack.service;

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
    public Bodega crearBodega(Bodega bodega, String username) {
        if (bodegaRepository.existsByNombre(bodega.getNombre())) {
            throw new IllegalArgumentException("Ya existe una bodega con el nombre: " + bodega.getNombre());
        }

        Usuario usuarioResponsable = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario responsable no encontrado: " + username));

        Bodega guardada = bodegaRepository.save(bodega);

        auditoriaService.registrarAuditoria(
                TipoOperacion.INSERT,
                usuarioResponsable,
                "Bodega",
                null,
                guardada.getNombre() + " (Ubicación: " + guardada.getUbicacion() + ", Capacidad: " + guardada.getCapacidad() + ")"
        );

        return guardada;
    }

    @Transactional
    public Bodega actualizarBodega(Long id, Bodega bodegaActualizada, String username) {
        Bodega bodegaExistente = obtenerPorId(id);
        
        Usuario usuarioResponsable = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario responsable no encontrado: " + username));
        
        String valorAnterior = bodegaExistente.getNombre() + " (Ubicación: " + bodegaExistente.getUbicacion() + ", Capacidad: " + bodegaExistente.getCapacidad() + ")";

        bodegaExistente.setNombre(bodegaActualizada.getNombre());
        bodegaExistente.setUbicacion(bodegaActualizada.getUbicacion());
        bodegaExistente.setCapacidad(bodegaActualizada.getCapacidad());
        bodegaExistente.setEncargado(bodegaActualizada.getEncargado());

        Bodega guardada = bodegaRepository.save(bodegaExistente);
        String valorNuevo = guardada.getNombre() + " (Ubicación: " + guardada.getUbicacion() + ", Capacidad: " + guardada.getCapacidad() + ")";

        auditoriaService.registrarAuditoria(
                TipoOperacion.UPDATE,
                usuarioResponsable,
                "Bodega",
                valorAnterior,
                valorNuevo
        );

        return guardada;
    }

    @Transactional
    public void eliminarBodega(Long id, String username) {
        Bodega bodega = obtenerPorId(id);
        
        Usuario usuarioResponsable = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario responsable no encontrado: " + username));
        
        String valorAnterior = bodega.getNombre() + " (Encargado ID: " + bodega.getEncargado().getId() + ")";

        bodegaRepository.delete(bodega);

        auditoriaService.registrarAuditoria(
                TipoOperacion.DELETE,
                usuarioResponsable,
                "Bodega",
                valorAnterior,
                null
        );
    }
}