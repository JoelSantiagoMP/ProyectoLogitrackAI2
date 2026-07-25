package com.example.logitrack.service;

import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.*;
import com.example.logitrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final BodegaRepository bodegaRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioBodegaRepository inventarioBodegaRepository;
    private final AuditoriaService auditoriaService;

    public MovimientoService(MovimientoRepository movimientoRepository,
            ProductoRepository productoRepository,
            BodegaRepository bodegaRepository,
            UsuarioRepository usuarioRepository,
            InventarioBodegaRepository inventarioBodegaRepository,
            AuditoriaService auditoriaService) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.bodegaRepository = bodegaRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventarioBodegaRepository = inventarioBodegaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Movimiento> obtenerTodos() {
        return movimientoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Movimiento obtenerPorId(Long id) {
        return movimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento no encontrado con id: " + id));
    }

    @Transactional
    public Movimiento registrarMovimiento(Movimiento movimiento, String username) {
        if (movimiento.getFecha() == null) {
            movimiento.setFecha(LocalDateTime.now());
        }

        Usuario usuarioResponsable = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
        movimiento.setUsuario(usuarioResponsable);

        Bodega origen = null;
        Bodega destino = null;

        if (movimiento.getBodegaOrigen() != null && movimiento.getBodegaOrigen().getId() != null) {
            origen = bodegaRepository.findById(movimiento.getBodegaOrigen().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bodega origen no encontrada"));
            movimiento.setBodegaOrigen(origen);
        }

        if (movimiento.getBodegaDestino() != null && movimiento.getBodegaDestino().getId() != null) {
            destino = bodegaRepository.findById(movimiento.getBodegaDestino().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bodega destino no encontrada"));
            movimiento.setBodegaDestino(destino);
        }

        validarBodegasPorTipo(movimiento.getTipoMovimiento(), origen, destino);

        if (movimiento.getDetalles() == null || movimiento.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("El movimiento debe contener detalles.");
        }

        StringBuilder resumenDetalles = new StringBuilder();

        for (DetalleMovimiento detalle : movimiento.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado: " + detalle.getProducto().getId()));

            detalle.setProducto(producto);
            detalle.setMovimiento(movimiento);
            int cantidad = detalle.getCantidad();

            if (movimiento.getTipoMovimiento() == TipoMovimiento.ENTRADA) {
                agregarInventario(destino, producto, cantidad);
            } else if (movimiento.getTipoMovimiento() == TipoMovimiento.SALIDA) {
                descontarInventario(origen, producto, cantidad);
            } else if (movimiento.getTipoMovimiento() == TipoMovimiento.TRANSFERENCIA) {
                descontarInventario(origen, producto, cantidad);
                agregarInventario(destino, producto, cantidad);
            }

            resumenDetalles.append("[").append(producto.getNombre()).append(": ").append(cantidad).append("] ");
        }

        Movimiento guardado = movimientoRepository.save(movimiento);

        auditoriaService.registrarAuditoria(
                TipoOperacion.INSERT, usuarioResponsable, "Movimiento", null,
                "Tipo: " + guardado.getTipoMovimiento() + ", Detalles: " + resumenDetalles.toString().trim());

        return guardado;
    }

    @Transactional(readOnly = true)
    public List<Movimiento> obtenerPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return movimientoRepository.findByFechaBetween(fechaInicio, fechaFin);
    }

    @Transactional(readOnly = true)
    public List<Movimiento> obtenerPorTipo(TipoMovimiento tipoMovimiento) {
        return movimientoRepository.findByTipoMovimiento(tipoMovimiento);
    }

    @Transactional(readOnly = true)
    public List<Movimiento> obtenerPorUsuarioId(Long usuarioId) {
        return movimientoRepository.findByUsuarioId(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<Movimiento> obtenerPorBodegaId(Long bodegaId) {
        return movimientoRepository.findByBodegaOrigenIdOrBodegaDestinoId(bodegaId, bodegaId);
    }

    private void validarBodegasPorTipo(TipoMovimiento tipo, Bodega origen, Bodega destino) {
        if (tipo == TipoMovimiento.ENTRADA && destino == null) {
            throw new IllegalArgumentException("Una entrada requiere bodega destino.");
        }
        if (tipo == TipoMovimiento.SALIDA && origen == null) {
            throw new IllegalArgumentException("Una salida requiere bodega origen.");
        }
        if (tipo == TipoMovimiento.TRANSFERENCIA && (origen == null || destino == null)) {
            throw new IllegalArgumentException("Transferencia requiere origen y destino.");
        }
    }

    private void agregarInventario(Bodega bodega, Producto producto, int cantidad) {
        InventarioBodega inv = inventarioBodegaRepository.findByBodegaIdAndProductoId(bodega.getId(), producto.getId())
                .orElseGet(() -> {
                    InventarioBodega nuevo = new InventarioBodega();
                    nuevo.setBodega(bodega);
                    nuevo.setProducto(producto);
                    nuevo.setCantidad(0);
                    return nuevo;
                });
        inv.setCantidad(inv.getCantidad() + cantidad);
        inventarioBodegaRepository.save(inv);
    }

    private void descontarInventario(Bodega bodega, Producto producto, int cantidad) {
        InventarioBodega inv = inventarioBodegaRepository.findByBodegaIdAndProductoId(bodega.getId(), producto.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto " + producto.getNombre() + " no tiene stock en la bodega origen."));

        if (inv.getCantidad() < cantidad) {
            throw new IllegalArgumentException("Stock insuficiente de " + producto.getNombre()
                    + " en la bodega. Disponible: " + inv.getCantidad());
        }
        inv.setCantidad(inv.getCantidad() - cantidad);
        inventarioBodegaRepository.save(inv);
    }
}