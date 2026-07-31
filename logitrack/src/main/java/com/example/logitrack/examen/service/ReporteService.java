package com.example.logitrack.examen.service; 

import com.example.logitrack.examen.repository.AuditoriaRepositoryE; 
import com.example.logitrack.examen.repository.MovimientoRepositoryE; 
import com.example.logitrack.model.Auditoria; 
import com.example.logitrack.model.Movimiento; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReporteService {

    @Autowired
    private MovimientoRepositoryE movimientoRepository;

    @Autowired
    private AuditoriaRepositoryE auditoriaRepository;

    public List<Movimiento> obtenerReporteMovimientos(String bodega, String producto, String tipoMovimiento, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return movimientoRepository.findByFiltrosOpcionales(bodega, producto, tipoMovimiento, fechaInicio, fechaFin);
    }

    public List<Auditoria> obtenerAuditoriasFiltradas(String entidadAfectada, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return auditoriaRepository.findByFiltrosOpcionales(entidadAfectada, fechaInicio, fechaFin);
    }
}

