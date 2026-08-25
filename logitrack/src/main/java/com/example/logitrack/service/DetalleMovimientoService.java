package com.example.logitrack.service;

import com.example.logitrack.model.DetalleMovimiento;
import com.example.logitrack.repository.DetalleMovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DetalleMovimientoService {

    private final DetalleMovimientoRepository detalleMovimientoRepository;

    public DetalleMovimientoService(DetalleMovimientoRepository detalleMovimientoRepository) {
        this.detalleMovimientoRepository = detalleMovimientoRepository;
    }

    @Transactional(readOnly = true)
    public List<DetalleMovimiento> obtenerTodos() {
        return detalleMovimientoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DetalleMovimiento> obtenerPorMovimientoId(Long movimientoId) {
        return detalleMovimientoRepository.findByMovimientoId(movimientoId);
    }

    @Transactional(readOnly = true)
    public List<Object[]> obtenerProductosMasMovidosRaw() {
        return detalleMovimientoRepository.findProductosMasMovidos();
    }
}
