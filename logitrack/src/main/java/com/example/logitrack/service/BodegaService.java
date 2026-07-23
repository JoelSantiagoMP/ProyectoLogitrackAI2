package com.example.logitrack.service;

import com.example.logitrack.model.Bodega;
import com.example.logitrack.repository.BodegaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BodegaService {

    @Autowired
    private BodegaRepository bodegaRepository;

    // Obtener todas las bodegas
    public List<Bodega> obtenerTodas() {
        return bodegaRepository.findAll();
    }

    // Obtener una bodega específica por su ID
    public Optional<Bodega> obtenerPorId(Long id) {
        return bodegaRepository.findById(id);
    }

    // Crear una nueva bodega o actualizar una existente
    public Bodega guardarBodega(Bodega bodega) {
        return bodegaRepository.save(bodega);
    }

    // Eliminar una bodega del sistema
    public void eliminarBodega(Long id) {
        bodegaRepository.deleteById(id);
    }
}
