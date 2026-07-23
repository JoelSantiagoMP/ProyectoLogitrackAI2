package com.example.logitrack.service;

import com.example.logitrack.exception.ResourceNotFoundException;
import com.example.logitrack.model.Producto;
import com.example.logitrack.model.TipoOperacion;
import com.example.logitrack.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final AuditoriaService auditoriaService;

    public ProductoService(ProductoRepository productoRepository, AuditoriaService auditoriaService) {
        this.productoRepository = productoRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Producto obtenerPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con el id: " + id));
    }

    @Transactional
    public Producto crearProducto(Producto producto, String username) {
        if (productoRepository.existsByNombre(producto.getNombre())) {
            throw new IllegalArgumentException("Ya existe un producto con el nombre: " + producto.getNombre());
        }
        Producto guardado = productoRepository.save(producto);

        auditoriaService.registrarAuditoria(
                TipoOperacion.INSERT,
                username,
                "Producto",
                guardado.getId(),
                null,
                guardado.getNombre() + " (Stock: " + guardado.getStock() + ", Precio: " + guardado.getPrecio() + ")"
        );

        return guardado;
    }

    @Transactional
    public Producto actualizarProducto(Long id, Producto productoActualizado, String username) {
        Producto productoExistente = obtenerPorId(id);
        String valorAnterior = productoExistente.getNombre() + " (Stock: " + productoExistente.getStock() + ", Precio: " + productoExistente.getPrecio() + ")";

        productoExistente.setNombre(productoActualizado.getNombre());
        productoExistente.setCategoria(productoActualizado.getCategoria());
        productoExistente.setStock(productoActualizado.getStock());
        productoExistente.setPrecio(productoActualizado.getPrecio());

        Producto guardado = productoRepository.save(productoExistente);
        String valorNuevo = guardado.getNombre() + " (Stock: " + guardado.getStock() + ", Precio: " + guardado.getPrecio() + ")";

        auditoriaService.registrarAuditoria(
                TipoOperacion.UPDATE,
                username,
                "Producto",
                guardado.getId(),
                valorAnterior,
                valorNuevo
        );

        return guardado;
    }

    @Transactional
    public void eliminarProducto(Long id, String username) {
        Producto producto = obtenerPorId(id);
        String valorAnterior = producto.getNombre() + " (Categoría: " + producto.getCategoria() + ")";

        productoRepository.delete(producto);

        auditoriaService.registrarAuditoria(
                TipoOperacion.DELETE,
                username,
                "Producto",
                id,
                valorAnterior,
                null
        );
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerStockBajo(Integer limiteStock) {
        int limite = (limiteStock != null) ? limiteStock : 10;
        return productoRepository.findByStockLessThan(limite);
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerPorCategoria(String categoria) {
        return productoRepository.findByCategoriaIgnoreCase(categoria);
    }
}
