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
    private final AuditoriaService auditoriaService;

    public MovimientoService(MovimientoRepository movimientoRepository,
                             ProductoRepository productoRepository,
                             BodegaRepository bodegaRepository,
                             UsuarioRepository usuarioRepository,
                             AuditoriaService auditoriaService) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.bodegaRepository = bodegaRepository;
        this.usuarioRepository = usuarioRepository;
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
        // 1. Establecer fecha actual si no viene especificada
        if (movimiento.getFecha() == null) {
            movimiento.setFecha(LocalDateTime.now());
        }

        // 2. Cargar usuario responsable
        Usuario usuarioResponsable;
        if (movimiento.getUsuario() != null && movimiento.getUsuario().getId() != null) {
            usuarioResponsable = usuarioRepository.findById(movimiento.getUsuario().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + movimiento.getUsuario().getId()));
        } else {
            usuarioResponsable = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con username: " + username));
        }
        movimiento.setUsuario(usuarioResponsable);

        // 3. Validar y asociar bodegas según tipo de movimiento
        if (movimiento.getBodegaOrigen() != null && movimiento.getBodegaOrigen().getId() != null) {
            Bodega origen = bodegaRepository.findById(movimiento.getBodegaOrigen().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bodega de origen no encontrada con id: " + movimiento.getBodegaOrigen().getId()));
            movimiento.setBodegaOrigen(origen);
        }

        if (movimiento.getBodegaDestino() != null && movimiento.getBodegaDestino().getId() != null) {
            Bodega destino = bodegaRepository.findById(movimiento.getBodegaDestino().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bodega de destino no encontrada con id: " + movimiento.getBodegaDestino().getId()));
            movimiento.setBodegaDestino(destino);
        }

        if (movimiento.getTipoMovimiento() == TipoMovimiento.TRANSFERENCIA) {
            if (movimiento.getBodegaOrigen() == null || movimiento.getBodegaDestino() == null) {
                throw new IllegalArgumentException("Una transferencia requiere especificar bodega de origen y destino.");
            }
        }

        // 4. Validar y actualizar stock de productos
        if (movimiento.getDetalles() == null || movimiento.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("El movimiento debe contener al menos un detalle de producto.");
        }

        StringBuilder resumenDetalles = new StringBuilder();

        for (DetalleMovimiento detalle : movimiento.getDetalles()) {
            if (detalle.getProducto() == null || detalle.getProducto().getId() == null) {
                throw new IllegalArgumentException("Cada detalle debe referenciar un producto existente con ID.");
            }

            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + detalle.getProducto().getId()));

            detalle.setProducto(producto);
            detalle.setMovimiento(movimiento);

            int cantidad = detalle.getCantidad();
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad del producto debe ser mayor a cero.");
            }

            if (movimiento.getTipoMovimiento() == TipoMovimiento.ENTRADA) {
                producto.setStock(producto.getStock() + cantidad);
            } else if (movimiento.getTipoMovimiento() == TipoMovimiento.SALIDA) {
                if (producto.getStock() < cantidad) {
                    throw new IllegalArgumentException("Stock insuficiente para el producto '" + producto.getNombre() +
                            "'. Disponible: " + producto.getStock() + ", Solicitado: " + cantidad);
                }
                producto.setStock(producto.getStock() - cantidad);
            }
            // En TRANSFERENCIA el stock global del producto no cambia, pero enlazamos las bodegas

            productoRepository.save(producto);

            resumenDetalles.append("[").append(producto.getNombre()).append(": ").append(cantidad).append("] ");
        }

        // 5. Guardar movimiento (cascada guarda los detalles)
        Movimiento guardado = movimientoRepository.save(movimiento);

        // 6. Registrar en auditoría
        auditoriaService.registrarAuditoria(
                TipoOperacion.INSERT,
                usuarioResponsable.getUsername(),
                "Movimiento",
                guardado.getId(),
                null,
                "Tipo: " + guardado.getTipoMovimiento() + ", Detalles: " + resumenDetalles.toString().trim()
        );

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
}
