package com.example.logitrack.repository;

import com.example.logitrack.model.Auditoria;
import com.example.logitrack.model.TipoOperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    // Auditorías por usuario responsable
    List<Auditoria> findByUsuarioIgnoreCase(String usuario);

    // Auditorías por tipo de operación (INSERT, UPDATE, DELETE)
    List<Auditoria> findByTipoOperacion(TipoOperacion tipoOperacion);

    // Auditorías por entidad afectada (ej: "Bodega", "Producto", "Movimiento")
    List<Auditoria> findByEntidadAfectadaIgnoreCase(String entidadAfectada);
}
