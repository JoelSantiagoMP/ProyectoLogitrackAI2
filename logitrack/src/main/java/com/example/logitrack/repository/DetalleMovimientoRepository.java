package com.example.logitrack.repository;

import com.example.logitrack.model.DetalleMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleMovimientoRepository extends JpaRepository<DetalleMovimiento, Long> {

    List<DetalleMovimiento> findByMovimientoId(Long movimientoId);

    // Consulta de productos más movidos (para reportes)
    @Query("SELECT d.producto.id, d.producto.nombre, SUM(d.cantidad) AS totalCantidad " +
           "FROM DetalleMovimiento d " +
           "GROUP BY d.producto.id, d.producto.nombre " +
           "ORDER BY totalCantidad DESC")
    List<Object[]> findProductosMasMovidos();
}
