package com.example.logitrack.repository;

import com.example.logitrack.model.Movimiento;
import com.example.logitrack.model.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByFechaBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    List<Movimiento> findByTipoMovimiento(TipoMovimiento tipoMovimiento);

    List<Movimiento> findByUsuarioId(Long usuarioId);

    List<Movimiento> findByBodegaOrigenIdOrBodegaDestinoId(Long bodegaOrigenId, Long bodegaDestinoId);

    @Query("""
            SELECT DISTINCT m FROM Movimiento m
            LEFT JOIN m.detalles d
            LEFT JOIN d.producto p
            LEFT JOIN m.bodegaOrigen bo
            LEFT JOIN m.bodegaDestino bd
            WHERE (:filtrarBodega = false OR LOWER(bo.nombre) = :bodega OR LOWER(bd.nombre) = :bodega)
              AND (:filtrarProducto = false OR LOWER(p.nombre) = :producto)
              AND (:filtrarTipo = false OR m.tipoMovimiento = :tipoMovimiento)
              AND (:filtrarFechaInicio = false OR m.fecha >= :fechaInicio)
              AND (:filtrarFechaFin = false OR m.fecha <= :fechaFin)
            ORDER BY m.fecha DESC
            """)
    List<Movimiento> findByFiltrosOpcionales(
            @Param("filtrarBodega") boolean filtrarBodega,
            @Param("bodega") String bodega,
            @Param("filtrarProducto") boolean filtrarProducto,
            @Param("producto") String producto,
            @Param("filtrarTipo") boolean filtrarTipo,
            @Param("tipoMovimiento") TipoMovimiento tipoMovimiento,
            @Param("filtrarFechaInicio") boolean filtrarFechaInicio,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("filtrarFechaFin") boolean filtrarFechaFin,
            @Param("fechaFin") LocalDateTime fechaFin);
}
