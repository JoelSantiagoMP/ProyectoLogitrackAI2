package com.example.logitrack.service;

import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.Producto;
import com.example.logitrack.model.TipoOperacion;
import com.example.logitrack.model.Usuario;
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

    public ProductoService(ProductoRepository productoRepository, AuditoriaService auditoriaService,
            UsuarioRepository usuarioRepository, InventarioBodegaRepository inventarioBodegaRepository) {
        this.productoRepository = productoRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
        this.inventarioBodegaRepository = inventarioBodegaRepository;
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
    public Producto crearProducto(Producto producto, String username) {
        if (productoRepository.existsByNombre(producto.getNombre())) {
            throw new IllegalArgumentException("Ya existe un producto con el nombre: " + producto.getNombre());
        }

        Usuario admin = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        Producto guardado = productoRepository.save(producto);
        guardado.setStock(0);

        auditoriaService.registrarAuditoria(
                TipoOperacion.INSERT, admin, "Producto", null,
                guardado.getNombre() + " (Precio: " + guardado.getPrecio() + ")");

        return guardado;
    }

    @Transactional
    public Producto actualizarProducto(Long id, Producto productoActualizado, String username) {
        Producto productoExistente = obtenerPorId(id);
        Usuario admin = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        String valorAnterior = productoExistente.getNombre() + " (Precio: " + productoExistente.getPrecio() + ")";

        productoExistente.setNombre(productoActualizado.getNombre());
        productoExistente.setCategoria(productoActualizado.getCategoria());
        productoExistente.setPrecio(productoActualizado.getPrecio());

        Producto guardado = productoRepository.save(productoExistente);
        calcularStockDinamico(guardado);

        String valorNuevo = guardado.getNombre() + " (Precio: " + guardado.getPrecio() + ")";

        auditoriaService.registrarAuditoria(
                TipoOperacion.UPDATE, admin, "Producto", valorAnterior, valorNuevo);

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
                TipoOperacion.DELETE, admin, "Producto", valorAnterior, null);
    }

    private void calcularStockDinamico(Producto producto) {
        Integer stockTotal = inventarioBodegaRepository.obtenerStockTotalPorProducto(producto.getId());
        producto.setStock(stockTotal != null ? stockTotal : 0);
    }
}