package com.example.logitrack.examen.repository;

import com.example.logitrack.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoRepositoryE extends JpaRepository<Movimiento, Long> {

       @Query("SELECT DISTINCT m FROM Movimiento m JOIN m.detalles d WHERE " +
       "(:bodega IS NULL OR m.bodegaOrigen.nombre = :bodega) AND " +
       "(:producto IS NULL OR d.producto.nombre = :producto) AND " +
       "(:tipoMovimiento IS NULL OR m.tipoMovimiento = :tipoMovimiento) AND " +
       "(:fechaInicio IS NULL OR m.fecha >= :fechaInicio) AND " +
       "(:fechaFin IS NULL OR m.fecha <= :fechaFin)")
List<Movimiento> findByFiltrosOpcionales(
        @Param("bodega") String bodega,
        @Param("producto") String producto,
        @Param("tipoMovimiento") String tipoMovimiento,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin);
}
