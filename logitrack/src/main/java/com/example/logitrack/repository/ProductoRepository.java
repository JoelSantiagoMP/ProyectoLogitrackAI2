package com.example.logitrack.repository;

import com.example.logitrack.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByNombre(String nombre);

    List<Producto> findAllByOrderByIdAsc();

    List<Producto> findByCategoriaIgnoreCaseOrderByIdAsc(String categoria);

    @Query("SELECT ib.producto FROM InventarioBodega ib GROUP BY ib.producto HAVING SUM(ib.cantidad) < :limite")
    List<Producto> findByStockLessThanCustom(@Param("limite") int limite);
}