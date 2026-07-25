package com.example.logitrack.repository;

import com.example.logitrack.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByNombre(String nombre);

    List<Producto> findByCategoriaIgnoreCase(String categoria);
}