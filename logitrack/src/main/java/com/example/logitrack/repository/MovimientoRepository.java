package com.example.logitrack.repository;

import com.example.logitrack.model.Movimiento;
import com.example.logitrack.model.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    // Movimientos por rango de fechas (BETWEEN)
    List<Movimiento> findByFechaBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    // Movimientos por tipo de movimiento (ENTRADA, SALIDA, TRANSFERENCIA)
    List<Movimiento> findByTipoMovimiento(TipoMovimiento tipoMovimiento);

    // Movimientos por usuario responsable
    List<Movimiento> findByUsuarioId(Long usuarioId);

    // Movimientos relacionados a una bodega específica (como origen o destino)
    List<Movimiento> findByBodegaOrigenIdOrBodegaDestinoId(Long bodegaOrigenId, Long bodegaDestinoId);
}
