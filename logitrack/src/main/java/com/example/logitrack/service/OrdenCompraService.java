package com.example.logitrack.service;

import com.example.logitrack.dto.CambioEstadoOrdenRequest;
import com.example.logitrack.dto.OrdenCompraRequest;
import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.Bodega;
import com.example.logitrack.model.DetalleMovimiento;
import com.example.logitrack.model.EstadoOrdenCompra;
import com.example.logitrack.model.Movimiento;
import com.example.logitrack.model.OrdenCompra;
import com.example.logitrack.model.Producto;
import com.example.logitrack.model.Proveedor;
import com.example.logitrack.model.TipoMovimiento;
import com.example.logitrack.model.TipoOperacion;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.BodegaRepository;
import com.example.logitrack.repository.OrdenCompraRepository;
import com.example.logitrack.repository.ProductoRepository;
import com.example.logitrack.repository.ProveedorRepository;
import com.example.logitrack.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final BodegaRepository bodegaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoService movimientoService;
    private final AuditoriaService auditoriaService;
    private final OrdenPdfService ordenPdfService;

    public OrdenCompraService(OrdenCompraRepository ordenCompraRepository,
            ProductoRepository productoRepository,
            ProveedorRepository proveedorRepository,
            BodegaRepository bodegaRepository,
            UsuarioRepository usuarioRepository,
            MovimientoService movimientoService,
            AuditoriaService auditoriaService,
            OrdenPdfService ordenPdfService) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.bodegaRepository = bodegaRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimientoService = movimientoService;
        this.auditoriaService = auditoriaService;
        this.ordenPdfService = ordenPdfService;
    }

    @Transactional(readOnly = true)
    public List<OrdenCompra> listar(EstadoOrdenCompra estado) {
        if (estado == null) {
            return ordenCompraRepository.findAll();
        }
        return ordenCompraRepository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public OrdenCompra obtenerPorId(Long id) {
        return ordenCompraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada con id: " + id));
    }

    @Transactional
    public OrdenCompra crear(OrdenCompraRequest request, String username) {
        if (request.getCantidad() == null || request.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0.");
        }
        Usuario autor = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        Bodega bodega = bodegaRepository.findById(request.getBodegaDestinoId())
                .orElseThrow(() -> new ResourceNotFoundException("Bodega destino no encontrada"));

        OrdenCompra orden = OrdenCompra.builder()
                .producto(producto)
                .proveedor(proveedor)
                .bodegaDestino(bodega)
                .cantidad(request.getCantidad())
                .precioUnitario(0.0)
                .total(0.0)
                .estado(EstadoOrdenCompra.BORRADOR)
                .creadoPor(autor)
                .build();
        aplicarPrecioProducto(orden);
        OrdenCompra guardada = ordenCompraRepository.save(orden);
        auditoriaService.registrarAuditoria(TipoOperacion.INSERT, autor, "OrdenCompra", guardada.getId(),
                null, "BORRADOR total=" + guardada.getTotal());
        return guardada;
    }

    @Transactional
    public byte[] generarPdf(Long id) {
        OrdenCompra orden = obtenerPorId(id);
        byte[] pdf = ordenPdfService.generar(orden);
        orden.setPdf(pdf);
        orden.setFechaGeneracionPdf(LocalDateTime.now(ZoneId.of("America/Bogota")));
        ordenCompraRepository.save(orden);
        return pdf;
    }

    @Transactional(readOnly = true)
    public byte[] obtenerPdf(Long id) {
        OrdenCompra orden = obtenerPorId(id);
        if (orden.getPdf() == null || orden.getPdf().length == 0) {
            throw new ResourceNotFoundException("La orden " + id + " aún no tiene PDF generado");
        }
        return orden.getPdf();
    }

    @Transactional
    public OrdenCompra cambiarEstado(Long id, CambioEstadoOrdenRequest request, String username, boolean esAdmin) {
        if (!esAdmin) {
            throw new AccessDeniedException("Solo un ADMIN puede aprobar, recibir o cancelar una orden.");
        }
        OrdenCompra orden = obtenerPorId(id);
        EstadoOrdenCompra actual = orden.getEstado();
        EstadoOrdenCompra destino = request.getEstado();
        if (destino == null) {
            throw new IllegalArgumentException("El estado destino es obligatorio.");
        }
        if (!transicionPermitida(actual, destino)) {
            throw new IllegalArgumentException(
                    "Transición inválida de " + actual + " a " + destino + ".");
        }

        Usuario actor = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        EstadoOrdenCompra anterior = actual;

        // Aprobar no altera inventario; sí corrige totales legacy mal calculados.
        if (anterior == EstadoOrdenCompra.BORRADOR && destino == EstadoOrdenCompra.APROBADA) {
            aplicarPrecioProducto(orden);
        }

        orden.setEstado(destino);
        orden.setPdf(null);
        orden.setFechaGeneracionPdf(null);

        // Solo al recibir se suma stock en la bodega destino (movimiento ENTRADA).
        if (anterior == EstadoOrdenCompra.APROBADA && destino == EstadoOrdenCompra.RECIBIDA) {
            registrarEntradaRecepcion(orden, username);
        }

        OrdenCompra guardada = ordenCompraRepository.save(orden);
        auditoriaService.registrarAuditoria(TipoOperacion.UPDATE, actor, "OrdenCompra", guardada.getId(),
                anterior.name(), destino.name());
        return guardada;
    }

    private boolean transicionPermitida(EstadoOrdenCompra actual, EstadoOrdenCompra destino) {
        if (actual == EstadoOrdenCompra.BORRADOR) {
            return destino == EstadoOrdenCompra.APROBADA || destino == EstadoOrdenCompra.CANCELADA;
        }
        if (actual == EstadoOrdenCompra.APROBADA) {
            return destino == EstadoOrdenCompra.RECIBIDA || destino == EstadoOrdenCompra.CANCELADA;
        }
        return false;
    }

    private void aplicarPrecioProducto(OrdenCompra orden) {
        Producto producto = orden.getProducto();
        Double precioProducto = producto != null ? producto.getPrecio() : null;
        if (precioProducto == null || precioProducto <= 0) {
            throw new IllegalArgumentException("El producto no tiene un precio unitario válido.");
        }
        if (orden.getCantidad() == null || orden.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0.");
        }
        orden.setPrecioUnitario(precioProducto);
        orden.setTotal(orden.getCantidad() * precioProducto);
    }

    private void registrarEntradaRecepcion(OrdenCompra orden, String username) {
        if (orden.getProducto() == null || orden.getProducto().getId() == null) {
            throw new IllegalArgumentException("La orden no tiene producto asociado.");
        }
        if (orden.getBodegaDestino() == null || orden.getBodegaDestino().getId() == null) {
            throw new IllegalArgumentException("La orden no tiene bodega destino.");
        }
        if (orden.getCantidad() == null || orden.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad de la orden debe ser mayor que 0.");
        }

        Long productoId = orden.getProducto().getId();
        Long bodegaId = orden.getBodegaDestino().getId();
        int cantidad = orden.getCantidad();
        int stockAntes = movimientoService.obtenerStockEnBodega(productoId, bodegaId);

        Movimiento movimiento = new Movimiento();
        movimiento.setTipoMovimiento(TipoMovimiento.ENTRADA);
        Bodega bodegaRef = new Bodega();
        bodegaRef.setId(bodegaId);
        movimiento.setBodegaDestino(bodegaRef);

        DetalleMovimiento detalle = new DetalleMovimiento();
        Producto productoRef = new Producto();
        productoRef.setId(productoId);
        detalle.setProducto(productoRef);
        detalle.setCantidad(cantidad);
        movimiento.setDetalles(new ArrayList<>(List.of(detalle)));

        movimientoService.registrarMovimiento(movimiento, username);

        int stockDespues = movimientoService.obtenerStockEnBodega(productoId, bodegaId);
        if (stockDespues != stockAntes + cantidad) {
            throw new IllegalStateException(
                    "La recepción no actualizó el inventario. Stock esperado: "
                            + (stockAntes + cantidad) + ", actual: " + stockDespues);
        }
    }
}
