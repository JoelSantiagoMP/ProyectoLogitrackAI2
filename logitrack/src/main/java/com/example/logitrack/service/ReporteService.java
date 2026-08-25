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

@Service
public class ReporteService {

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
    public List<Movimiento> obtenerReporteMovimientos(String bodega, String producto,
            TipoMovimiento tipoMovimiento, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        String bodegaFiltro = normalize(bodega);
        String productoFiltro = normalize(producto);
        LocalDateTime fechaInicioSafe = fechaInicio != null ? fechaInicio : LocalDateTime.now();
        LocalDateTime fechaFinSafe = fechaFin != null ? fechaFin : LocalDateTime.now();
        TipoMovimiento tipoSafe = tipoMovimiento != null ? tipoMovimiento : TipoMovimiento.ENTRADA;
        return movimientoRepository.findByFiltrosOpcionales(
                bodegaFiltro != null,
                bodegaFiltro != null ? bodegaFiltro : "",
                productoFiltro != null,
                productoFiltro != null ? productoFiltro : "",
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
        String entidadFiltro = normalize(entidadAfectada);
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
    public List<InventarioReporteDTO> obtenerReporteInventario(Long bodegaId) {
        return inventarioBodegaRepository.findInventarioParaReporte(bodegaId).stream()
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

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
