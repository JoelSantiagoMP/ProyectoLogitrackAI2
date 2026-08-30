package com.example.logitrack.service;

import com.example.logitrack.dto.AccionPanelRequest;
import com.example.logitrack.dto.AlertaPanelRequest;
import com.example.logitrack.dto.PanelResumenRequest;
import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.ResumenPanel;
import com.example.logitrack.model.TipoOperacion;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.BodegaRepository;
import com.example.logitrack.repository.OrdenCompraRepository;
import com.example.logitrack.repository.ProductoRepository;
import com.example.logitrack.repository.ResumenPanelRepository;
import com.example.logitrack.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class PanelResumenService {

    private final ResumenPanelRepository resumenPanelRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final BodegaRepository bodegaRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PanelResumenService(ResumenPanelRepository resumenPanelRepository,
            UsuarioRepository usuarioRepository,
            ProductoRepository productoRepository,
            BodegaRepository bodegaRepository,
            OrdenCompraRepository ordenCompraRepository,
            AuditoriaService auditoriaService) {
        this.resumenPanelRepository = resumenPanelRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.bodegaRepository = bodegaRepository;
        this.ordenCompraRepository = ordenCompraRepository;
        this.auditoriaService = auditoriaService;
        this.objectMapper.findAndRegisterModules();
    }

    @Transactional(readOnly = true)
    public ResumenPanel obtenerUltimo() {
        return resumenPanelRepository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new ResourceNotFoundException("No hay un resumen de panel publicado."));
    }

    @Transactional(readOnly = true)
    public PanelResumenRequest obtenerUltimoContenido() {
        ResumenPanel resumen = obtenerUltimo();
        try {
            return objectMapper.readValue(resumen.getContenidoJson(), PanelResumenRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("El resumen almacenado no es JSON válido.");
        }
    }

    @Transactional
    public ResumenPanel publicar(PanelResumenRequest request, String username) {
        validarContrato(request);
        Usuario autor = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("El resumen no se pudo serializar.");
        }

        ResumenPanel existente = resumenPanelRepository.findByFecha(request.getFecha()).orElse(null);
        String anterior = existente != null ? existente.getContenidoJson() : null;
        ResumenPanel guardado;
        if (existente != null) {
            existente.setContenidoJson(json);
            existente.setAutor(autor);
            guardado = resumenPanelRepository.save(existente);
        } else {
            guardado = resumenPanelRepository.save(ResumenPanel.builder()
                    .fecha(request.getFecha())
                    .contenidoJson(json)
                    .autor(autor)
                    .build());
        }
        auditoriaService.registrarAuditoria(TipoOperacion.UPDATE, autor, "ResumenPanel", guardado.getId(),
                anterior, json);
        return guardado;
    }

    private void validarContrato(PanelResumenRequest request) {
        LocalDate hoy = LocalDate.now(IndicadoresInventarioService.ZONA_BOGOTA);
        if (request.getFecha() == null || !request.getFecha().equals(hoy)) {
            throw new IllegalArgumentException("La fecha debe ser la fecha actual en America/Bogota.");
        }
        if (request.getNarrativa() == null || request.getNarrativa().length() < 20
                || request.getNarrativa().length() > 500) {
            throw new IllegalArgumentException("La narrativa debe tener entre 20 y 500 caracteres.");
        }
        if (request.getAlertas() == null || request.getAccionesSugeridas() == null) {
            throw new IllegalArgumentException("alertas y accionesSugeridas son obligatorios.");
        }
        for (AlertaPanelRequest alerta : request.getAlertas()) {
            validarAlerta(alerta);
        }
        for (AccionPanelRequest accion : request.getAccionesSugeridas()) {
            validarAccion(accion);
        }
    }

    private void validarAlerta(AlertaPanelRequest alerta) {
        if (alerta.getSeveridad() == null) {
            throw new IllegalArgumentException("Severidad inválida. Use BAJA, MEDIA o ALTA.");
        }
        int enlaces = contarIds(alerta.getProductoId(), alerta.getOrdenId(), alerta.getBodegaId());
        if (enlaces < 1) {
            throw new IllegalArgumentException("Cada alerta debe enlazar al menos un identificador.");
        }
        validarExistenciaIds(alerta.getProductoId(), alerta.getOrdenId(), alerta.getBodegaId());
    }

    private void validarAccion(AccionPanelRequest accion) {
        if (accion.getTipo() == null) {
            throw new IllegalArgumentException("Tipo de acción inválido.");
        }
        int enlaces = contarIds(accion.getProductoId(), accion.getOrdenId(), accion.getBodegaId());
        if (enlaces != 1) {
            throw new IllegalArgumentException("Cada acción debe enlazar exactamente un identificador.");
        }
        validarExistenciaIds(accion.getProductoId(), accion.getOrdenId(), accion.getBodegaId());
    }

    private int contarIds(Long productoId, Long ordenId, Long bodegaId) {
        int n = 0;
        if (productoId != null) {
            n++;
        }
        if (ordenId != null) {
            n++;
        }
        if (bodegaId != null) {
            n++;
        }
        return n;
    }

    private void validarExistenciaIds(Long productoId, Long ordenId, Long bodegaId) {
        if (productoId != null && !productoRepository.existsById(productoId)) {
            throw new IllegalArgumentException("productoId no existe: " + productoId);
        }
        if (ordenId != null && !ordenCompraRepository.existsById(ordenId)) {
            throw new IllegalArgumentException("ordenId no existe: " + ordenId);
        }
        if (bodegaId != null && !bodegaRepository.existsById(bodegaId)) {
            throw new IllegalArgumentException("bodegaId no existe: " + bodegaId);
        }
    }
}
