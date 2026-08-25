package com.example.logitrack.service;

import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.*;
import com.example.logitrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        validarDetallesYStockDisponible(movimiento.getTipoMovimiento(), origen, movimiento.getDetalles());

        StringBuilder resumenDetalles = new StringBuilder();

        for (DetalleMovimiento detalle : movimiento.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado: " + detalle.getProducto().getId()));

            detalle.setProducto(producto);
            detalle.setMovimiento(movimiento);
            int cantidad = detalle.getCantidad();
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad del detalle debe ser mayor a cero.");
            }

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
                TipoOperacion.INSERT, usuarioResponsable, "Movimiento", guardado.getId(), null,
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

    @Transactional(readOnly = true)
    public int obtenerStockEnBodega(Long productoId, Long bodegaId) {
        return inventarioBodegaRepository.findByBodegaIdAndProductoId(bodegaId, productoId)
                .map(InventarioBodega::getCantidad)
                .orElse(0);
    }

    @Transactional
    public void ajustarStockEnBodega(Long productoId, Long bodegaId, int cantidadObjetivo, String username) {
        if (cantidadObjetivo < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }
        productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        bodegaRepository.findById(bodegaId)
                .orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada con id: " + bodegaId));

        int actual = obtenerStockEnBodega(productoId, bodegaId);
        int delta = cantidadObjetivo - actual;
        if (delta == 0) {
            return;
        }

        Movimiento movimiento = new Movimiento();
        Bodega bodegaRef = new Bodega();
        bodegaRef.setId(bodegaId);
        if (delta > 0) {
            movimiento.setTipoMovimiento(TipoMovimiento.ENTRADA);
            movimiento.setBodegaDestino(bodegaRef);
        } else {
            movimiento.setTipoMovimiento(TipoMovimiento.SALIDA);
            movimiento.setBodegaOrigen(bodegaRef);
        }

        DetalleMovimiento detalle = new DetalleMovimiento();
        Producto productoRef = new Producto();
        productoRef.setId(productoId);
        detalle.setProducto(productoRef);
        detalle.setCantidad(Math.abs(delta));
        movimiento.setDetalles(new java.util.ArrayList<>(java.util.List.of(detalle)));

        registrarMovimiento(movimiento, username);
    }

    private void validarBodegasPorTipo(TipoMovimiento tipo, Bodega origen, Bodega destino) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio.");
        }
        if (tipo == TipoMovimiento.ENTRADA && destino == null) {
            throw new IllegalArgumentException("Una entrada requiere bodega destino.");
        }
        if (tipo == TipoMovimiento.SALIDA && origen == null) {
            throw new IllegalArgumentException("Una salida requiere bodega origen.");
        }
        if (tipo == TipoMovimiento.TRANSFERENCIA && (origen == null || destino == null)) {
            throw new IllegalArgumentException("Transferencia requiere origen y destino.");
        }
        if (tipo == TipoMovimiento.TRANSFERENCIA && origen.getId().equals(destino.getId())) {
            throw new IllegalArgumentException("La bodega de origen y destino no pueden ser la misma.");
        }
    }

    private void validarDetallesYStockDisponible(TipoMovimiento tipo, Bodega origen,
            List<DetalleMovimiento> detalles) {
        Map<Long, Integer> cantidadPorProducto = new HashMap<>();
        for (DetalleMovimiento detalle : detalles) {
            if (detalle.getProducto() == null || detalle.getProducto().getId() == null) {
                throw new IllegalArgumentException("Cada detalle debe indicar el producto.");
            }
            if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad de cada detalle debe ser mayor a cero.");
            }
            cantidadPorProducto.merge(detalle.getProducto().getId(), detalle.getCantidad(), Integer::sum);
        }

        if (tipo != TipoMovimiento.SALIDA && tipo != TipoMovimiento.TRANSFERENCIA) {
            return;
        }

        for (Map.Entry<Long, Integer> entrada : cantidadPorProducto.entrySet()) {
            Long productoId = entrada.getKey();
            int cantidadSolicitada = entrada.getValue();
            int disponible = obtenerStockEnBodega(productoId, origen.getId());
            if (disponible < cantidadSolicitada) {
                Producto producto = productoRepository.findById(productoId).orElse(null);
                String nombre = producto != null ? producto.getNombre() : ("ID " + productoId);
                throw new IllegalArgumentException("Stock insuficiente de " + nombre
                        + " en la bodega origen. Disponible: " + disponible
                        + ", solicitado: " + cantidadSolicitada);
            }
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
        int nuevoStock = inv.getCantidad() - cantidad;
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("La operación dejaría stock negativo de " + producto.getNombre() + ".");
        }
        inv.setCantidad(nuevoStock);
        inventarioBodegaRepository.save(inv);
    }
}