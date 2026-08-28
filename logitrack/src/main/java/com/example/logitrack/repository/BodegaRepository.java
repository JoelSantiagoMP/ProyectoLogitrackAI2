package com.example.logitrack.repository;

import com.example.logitrack.model.Bodega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BodegaRepository extends JpaRepository<Bodega, Long> {
    
    Optional<Bodega> findByNombre(String nombre);
    
    boolean existsByNombre(String nombre);

    List<Bodega> findAllByOrderByIdAsc();
}
