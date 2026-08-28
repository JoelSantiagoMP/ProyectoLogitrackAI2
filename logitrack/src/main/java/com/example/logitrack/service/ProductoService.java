package com.example.logitrack.service;

import com.example.logitrack.dto.ProductoRequest;
import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.Bodega;
import com.example.logitrack.model.InventarioBodega;
import com.example.logitrack.model.Producto;
import com.example.logitrack.model.Proveedor;
import com.example.logitrack.model.TipoOperacion;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.BodegaRepository;
import com.example.logitrack.repository.InventarioBodegaRepository;
import com.example.logitrack.repository.ProductoRepository;
import com.example.logitrack.repository.ProveedorRepository;
import com.example.logitrack.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;
    private final InventarioBodegaRepository inventarioBodegaRepository;
    private final BodegaRepository bodegaRepository;
    private final ProveedorRepository proveedorRepository;
    private final MovimientoService movimientoService;

    public ProductoService(ProductoRepository productoRepository, AuditoriaService auditoriaService,
            UsuarioRepository usuarioRepository, InventarioBodegaRepository inventarioBodegaRepository,
            BodegaRepository bodegaRepository, ProveedorRepository proveedorRepository,
            MovimientoService movimientoService) {
        this.productoRepository = productoRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
        this.inventarioBodegaRepository = inventarioBodegaRepository;
        this.bodegaRepository = bodegaRepository;
        this.proveedorRepository = proveedorRepository;
        this.movimientoService = movimientoService;
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerTodos() {
        List<Producto> productos = productoRepository.findAll();
        enriquecerProductos(productos);
        return productos;
    }

    @Transactional(readOnly = true)
    public Producto obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con el id: " + id));
        enriquecerProductos(List.of(producto));
        return producto;
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerPorCategoria(String categoria) {
        List<Producto> productos = productoRepository.findByCategoriaIgnoreCase(categoria);
        enriquecerProductos(productos);
        return productos;
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerProductosStockBajo() {
        List<Producto> productos = productoRepository.findAll();
        enriquecerProductos(productos);
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
        asignarProveedorPrincipal(producto, request.getProveedorId());

        Producto guardado = productoRepository.save(producto);

        int stockInicial = request.getStock() != null ? request.getStock() : 0;
        if (stockInicial > 0) {
            movimientoService.ajustarStockEnBodega(guardado.getId(), request.getBodegaId(), stockInicial, username);
        } else {
            registrarInventarioEnBodega(guardado, request.getBodegaId(), 0);
        }
        enriquecerProductos(List.of(guardado));

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
        asignarProveedorPrincipal(productoExistente, request.getProveedorId());

        Producto guardado = productoRepository.save(productoExistente);

        if (request.getStock() != null) {
            movimientoService.ajustarStockEnBodega(guardado.getId(), request.getBodegaId(), request.getStock(),
                    username);
        } else {
            registrarInventarioEnBodega(guardado, request.getBodegaId(), 0);
        }

        enriquecerProductos(List.of(guardado));

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

    private void enriquecerProductos(List<Producto> productos) {
        if (productos == null || productos.isEmpty()) {
            return;
        }
        productos.forEach(this::calcularStockDinamico);

        List<Long> productoIds = productos.stream().map(Producto::getId).toList();
        List<InventarioBodega> inventarios = inventarioBodegaRepository.findByProductoIdIn(productoIds);
        Map<Long, List<InventarioBodega>> inventarioPorProducto = inventarios.stream()
                .collect(Collectors.groupingBy(inv -> inv.getProducto().getId()));

        for (Producto producto : productos) {
            List<InventarioBodega> items = inventarioPorProducto.getOrDefault(producto.getId(), List.of());
            if (items.isEmpty()) {
                producto.setBodegaId(null);
                producto.setBodegaNombre(null);
                continue;
            }

            String nombres = items.stream()
                    .map(inv -> inv.getBodega().getNombre())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.joining(", "));
            producto.setBodegaNombre(nombres);

            InventarioBodega principal = items.stream()
                    .max(Comparator.comparing(InventarioBodega::getCantidad)
                            .thenComparing(inv -> inv.getBodega().getNombre(), String.CASE_INSENSITIVE_ORDER))
                    .orElse(items.get(0));
            producto.setBodegaId(principal.getBodega().getId());
        }
    }

    private void asignarProveedorPrincipal(Producto producto, Long proveedorId) {
        if (proveedorId == null) {
            producto.setProveedorPrincipal(null);
            return;
        }
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proveedor no encontrado con el id: " + proveedorId));
        producto.setProveedorPrincipal(proveedor);
    }

    private void registrarInventarioEnBodega(Producto producto, Long bodegaId, int cantidad) {
        if (inventarioBodegaRepository.findByBodegaIdAndProductoId(bodegaId, producto.getId()).isPresent()) {
            return;
        }
        Bodega bodega = bodegaRepository.findById(bodegaId)
                .orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada con id: " + bodegaId));

        InventarioBodega inventario = new InventarioBodega();
        inventario.setBodega(bodega);
        inventario.setProducto(producto);
        inventario.setCantidad(cantidad);
        inventarioBodegaRepository.save(inventario);
    }
}