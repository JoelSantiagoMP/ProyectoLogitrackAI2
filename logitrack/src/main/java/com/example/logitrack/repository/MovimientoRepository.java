package com.example.logitrack.repository;

import com.example.logitrack.model.Movimiento;
import com.example.logitrack.model.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByFechaBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    List<Movimiento> findByFechaGreaterThanEqualAndFechaLessThan(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    @Query(value = """
            SELECT m.*
            FROM logitrack.movimiento m
            WHERE CAST(((m.fecha AT TIME ZONE 'UTC') AT TIME ZONE 'America/Bogota') AS date) >= :desde
              AND CAST(((m.fecha AT TIME ZONE 'UTC') AT TIME ZONE 'America/Bogota') AS date) < :hasta
            """, nativeQuery = true)
    List<Movimiento> findByFechaCalendarioBogota(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    List<Movimiento> findByTipoMovimiento(TipoMovimiento tipoMovimiento);

    List<Movimiento> findByUsuarioId(Long usuarioId);

    List<Movimiento> findByBodegaOrigenIdOrBodegaDestinoId(Long bodegaOrigenId, Long bodegaDestinoId);

    @Query("""
            SELECT DISTINCT m FROM Movimiento m
            LEFT JOIN m.detalles d
            LEFT JOIN d.producto p
            LEFT JOIN m.bodegaOrigen bo
            LEFT JOIN m.bodegaDestino bd
            WHERE (:filtrarBodega = false OR bo.id = :bodegaId OR bd.id = :bodegaId)
              AND (:filtrarProducto = false OR p.id = :productoId)
              AND (:filtrarTipo = false OR m.tipoMovimiento = :tipoMovimiento)
              AND (:filtrarFechaInicio = false OR m.fecha >= :fechaInicio)
              AND (:filtrarFechaFin = false OR m.fecha <= :fechaFin)
            ORDER BY m.fecha DESC
            """)
    List<Movimiento> findByFiltrosOpcionales(
            @Param("filtrarBodega") boolean filtrarBodega,
            @Param("bodegaId") Long bodegaId,
            @Param("filtrarProducto") boolean filtrarProducto,
            @Param("productoId") Long productoId,
            @Param("filtrarTipo") boolean filtrarTipo,
            @Param("tipoMovimiento") TipoMovimiento tipoMovimiento,
            @Param("filtrarFechaInicio") boolean filtrarFechaInicio,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("filtrarFechaFin") boolean filtrarFechaFin,
            @Param("fechaFin") LocalDateTime fechaFin);
}
