package com.example.logitrack.service;

import com.example.logitrack.dto.AuditoriaDTO;
import com.example.logitrack.dto.InventarioReporteDTO;
import com.example.logitrack.model.InventarioBodega;
import com.example.logitrack.model.Movimiento;
import com.example.logitrack.model.TipoMovimiento;
import com.example.logitrack.repository.AuditoriaRepository;
import com.example.logitrack.repository.InventarioBodegaRepository;
import com.example.logitrack.repository.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ReporteService {

    private static final Set<String> ENTIDADES_AUDITORIA = Set.of(
            "producto", "bodega", "movimiento", "ordencompra", "usuario", "resumenpanel");

    private final MovimientoRepository movimientoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final InventarioBodegaRepository inventarioBodegaRepository;

    public ReporteService(MovimientoRepository movimientoRepository,
            AuditoriaRepository auditoriaRepository,
            InventarioBodegaRepository inventarioBodegaRepository) {
        this.movimientoRepository = movimientoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.inventarioBodegaRepository = inventarioBodegaRepository;
    }

    @Transactional(readOnly = true)
    public List<Movimiento> obtenerReporteMovimientos(Long bodegaId, Long productoId,
            TipoMovimiento tipoMovimiento, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        validarRangoFechas(fechaInicio, fechaFin);
        LocalDateTime fechaInicioSafe = fechaInicio != null ? fechaInicio : LocalDateTime.now();
        LocalDateTime fechaFinSafe = fechaFin != null ? fechaFin : LocalDateTime.now();
        TipoMovimiento tipoSafe = tipoMovimiento != null ? tipoMovimiento : TipoMovimiento.ENTRADA;
        return movimientoRepository.findByFiltrosOpcionales(
                bodegaId != null,
                bodegaId != null ? bodegaId : 0L,
                productoId != null,
                productoId != null ? productoId : 0L,
                tipoMovimiento != null,
                tipoSafe,
                fechaInicio != null,
                fechaInicioSafe,
                fechaFin != null,
                fechaFinSafe);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> obtenerAuditoriasFiltradas(String entidadAfectada,
            LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        validarRangoFechas(fechaInicio, fechaFin);
        String entidadFiltro = normalizeEntidad(entidadAfectada);
        LocalDateTime fechaInicioSafe = fechaInicio != null ? fechaInicio : LocalDateTime.now();
        LocalDateTime fechaFinSafe = fechaFin != null ? fechaFin : LocalDateTime.now();
        return auditoriaRepository.findByFiltrosOpcionales(
                entidadFiltro != null,
                entidadFiltro != null ? entidadFiltro : "",
                fechaInicio != null,
                fechaInicioSafe,
                fechaFin != null,
                fechaFinSafe)
                .stream()
                .map(AuditoriaDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventarioReporteDTO> obtenerReporteInventario(Long bodegaId, Long productoId, String categoria) {
        String categoriaFiltro = normalize(categoria);
        return inventarioBodegaRepository
                .findInventarioParaReporte(
                        bodegaId,
                        productoId,
                        categoriaFiltro != null,
                        categoriaFiltro != null ? categoriaFiltro : "")
                .stream()
                .map(this::toInventarioDto)
                .toList();
    }

    private InventarioReporteDTO toInventarioDto(InventarioBodega inventario) {
        return new InventarioReporteDTO(
                inventario.getBodega().getId(),
                inventario.getBodega().getNombre(),
                inventario.getProducto().getId(),
                inventario.getProducto().getNombre(),
                inventario.getProducto().getCategoria(),
                inventario.getCantidad());
    }

    private void validarRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }

    private String normalizeEntidad(String entidadAfectada) {
        String normalizada = normalize(entidadAfectada);
        if (normalizada == null) {
            return null;
        }
        if (!ENTIDADES_AUDITORIA.contains(normalizada)) {
            throw new IllegalArgumentException(
                    "Entidad de auditoría no válida. Use una de las opciones del selector.");
        }
        return normalizada;
    }
}
