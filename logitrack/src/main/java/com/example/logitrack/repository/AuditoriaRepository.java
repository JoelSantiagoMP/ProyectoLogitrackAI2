package com.example.logitrack.repository;

import com.example.logitrack.model.Auditoria;
import com.example.logitrack.model.TipoOperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    
    // Spring Data convierte automáticamente esto en un JOIN con la tabla usuario buscando por el username
    List<Auditoria> findByUsuarioResponsableUsernameIgnoreCase(String username);
    
    List<Auditoria> findByTipoOperacion(TipoOperacion tipoOperacion);
    
    List<Auditoria> findByEntidadAfectadaIgnoreCase(String entidadAfectada);
}