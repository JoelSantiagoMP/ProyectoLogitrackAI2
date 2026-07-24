package com.example.logitrack.repository;

import com.example.logitrack.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    // Consulta para obtener productos con stock bajo (menor a un valor específico)
    List<Producto> findByStockLessThan(Integer limiteStock);

    // Consulta de productos por categoría
    List<Producto> findByCategoriaIgnoreCase(String categoria);

    boolean existsByNombre(String nombre);
}
