package com.example.logitrack.repository;

import com.example.logitrack.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}
