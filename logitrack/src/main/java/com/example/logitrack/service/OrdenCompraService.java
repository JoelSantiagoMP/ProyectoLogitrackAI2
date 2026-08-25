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

    public OrdenCompraService(OrdenCompraRepository ordenCompraRepository,
            ProductoRepository productoRepository,
            ProveedorRepository proveedorRepository,
            BodegaRepository bodegaRepository,
            UsuarioRepository usuarioRepository,
            MovimientoService movimientoService,
            AuditoriaService auditoriaService) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.bodegaRepository = bodegaRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimientoService = movimientoService;
        this.auditoriaService = auditoriaService;
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

        double total = request.getCantidad() * request.getPrecioUnitario();
        OrdenCompra orden = OrdenCompra.builder()
                .producto(producto)
                .proveedor(proveedor)
                .bodegaDestino(bodega)
                .cantidad(request.getCantidad())
                .precioUnitario(request.getPrecioUnitario())
                .total(total)
                .estado(EstadoOrdenCompra.BORRADOR)
                .creadoPor(autor)
                .build();
        OrdenCompra guardada = ordenCompraRepository.save(orden);
        auditoriaService.registrarAuditoria(TipoOperacion.INSERT, autor, "OrdenCompra", guardada.getId(),
                null, "BORRADOR total=" + total);
        return guardada;
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
        orden.setEstado(destino);
        orden.setPdf(null);
        orden.setFechaGeneracionPdf(null);

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

    private void registrarEntradaRecepcion(OrdenCompra orden, String username) {
        Movimiento movimiento = new Movimiento();
        movimiento.setTipoMovimiento(TipoMovimiento.ENTRADA);
        movimiento.setBodegaDestino(orden.getBodegaDestino());
        DetalleMovimiento detalle = new DetalleMovimiento();
        detalle.setProducto(orden.getProducto());
        detalle.setCantidad(orden.getCantidad());
        movimiento.setDetalles(new ArrayList<>(List.of(detalle)));
        movimientoService.registrarMovimiento(movimiento, username);
    }
}
