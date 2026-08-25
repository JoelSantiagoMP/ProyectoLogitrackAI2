package com.example.logitrack.service;

import com.example.logitrack.dto.ProductoRequest;
import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.Producto;
import com.example.logitrack.model.TipoOperacion;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.BodegaRepository;
import com.example.logitrack.repository.InventarioBodegaRepository;
import com.example.logitrack.repository.ProductoRepository;
import com.example.logitrack.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;
    private final InventarioBodegaRepository inventarioBodegaRepository;
    private final BodegaRepository bodegaRepository;
    private final MovimientoService movimientoService;

    public ProductoService(ProductoRepository productoRepository, AuditoriaService auditoriaService,
            UsuarioRepository usuarioRepository, InventarioBodegaRepository inventarioBodegaRepository,
            BodegaRepository bodegaRepository, MovimientoService movimientoService) {
        this.productoRepository = productoRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
        this.inventarioBodegaRepository = inventarioBodegaRepository;
        this.bodegaRepository = bodegaRepository;
        this.movimientoService = movimientoService;
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerTodos() {
        List<Producto> productos = productoRepository.findAll();
        productos.forEach(this::calcularStockDinamico);
        return productos;
    }

    @Transactional(readOnly = true)
    public Producto obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con el id: " + id));
        calcularStockDinamico(producto);
        return producto;
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerPorCategoria(String categoria) {
        List<Producto> productos = productoRepository.findByCategoriaIgnoreCase(categoria);
        productos.forEach(this::calcularStockDinamico);
        return productos;
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerProductosStockBajo() {
        // Al ser @Transient, traemos los registros, calculamos en memoria y filtramos
        List<Producto> productos = productoRepository.findAll();
        productos.forEach(this::calcularStockDinamico);
        return productos.stream()
                .filter(p -> p.getStock() < 10)
                .toList();
    }

    @Transactional
    public Producto crearProducto(ProductoRequest request, String username) {
        if (productoRepository.existsByNombre(request.getNombre())) {
            throw new IllegalArgumentException("Ya existe un producto con el nombre: " + request.getNombre());
        }

        Usuario admin = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setCategoria(request.getCategoria());
        producto.setPrecio(request.getPrecio());

        Producto guardado = productoRepository.save(producto);

        int stockInicial = request.getStock() != null ? request.getStock() : 0;
        if (stockInicial > 0) {
            if (request.getBodegaId() == null) {
                throw new IllegalArgumentException(
                        "Para asignar stock inicial debes indicar la bodega donde se registrará el inventario.");
            }
            movimientoService.ajustarStockEnBodega(guardado.getId(), request.getBodegaId(), stockInicial, username);
        }
        calcularStockDinamico(guardado);

        auditoriaService.registrarAuditoria(
                TipoOperacion.INSERT, admin, "Producto", guardado.getId(), null,
                guardado.getNombre() + " (Precio: " + guardado.getPrecio() + ", Stock inicial: " + stockInicial + ")");

        return guardado;
    }

    @Transactional
    public Producto actualizarProducto(Long id, ProductoRequest request, String username) {
        Producto productoExistente = obtenerPorId(id);
        Usuario admin = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        String valorAnterior = productoExistente.getNombre() + " (Precio: " + productoExistente.getPrecio()
                + ", Stock total: " + productoExistente.getStock() + ")";

        productoExistente.setNombre(request.getNombre());
        productoExistente.setCategoria(request.getCategoria());
        productoExistente.setPrecio(request.getPrecio());

        Producto guardado = productoRepository.save(productoExistente);

        if (request.getStock() != null) {
            if (request.getBodegaId() == null) {
                throw new IllegalArgumentException(
                        "Para ajustar el stock debes indicar la bodega del inventario.");
            }
            movimientoService.ajustarStockEnBodega(guardado.getId(), request.getBodegaId(), request.getStock(),
                    username);
        }

        calcularStockDinamico(guardado);

        String valorNuevo = guardado.getNombre() + " (Precio: " + guardado.getPrecio()
                + ", Stock total: " + guardado.getStock() + ")";

        auditoriaService.registrarAuditoria(
                TipoOperacion.UPDATE, admin, "Producto", guardado.getId(), valorAnterior, valorNuevo);

        return guardado;
    }

    @Transactional
    public void eliminarProducto(Long id, String username) {
        Producto producto = obtenerPorId(id);
        Usuario admin = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        String valorAnterior = producto.getNombre() + " (Categoría: " + producto.getCategoria() + ")";
        productoRepository.delete(producto);

        auditoriaService.registrarAuditoria(
                TipoOperacion.DELETE, admin, "Producto", producto.getId(), valorAnterior, null);
    }

    @Transactional(readOnly = true)
    public int obtenerStockEnBodega(Long productoId, Long bodegaId) {
        obtenerPorId(productoId);
        bodegaRepository.findById(bodegaId)
                .orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada con id: " + bodegaId));
        return movimientoService.obtenerStockEnBodega(productoId, bodegaId);
    }

    private void calcularStockDinamico(Producto producto) {
        Integer stockTotal = inventarioBodegaRepository.obtenerStockTotalPorProducto(producto.getId());
        producto.setStock(stockTotal != null ? stockTotal : 0);
    }
}