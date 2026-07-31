package com.example.logitrack.examen.repository;

import com.example.logitrack.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaRepositoryE extends JpaRepository<Auditoria, Long> {

    @Query("SELECT a FROM Auditoria a WHERE " +
           "(:entidadAfectada IS NULL OR a.entidadAfectada = :entidadAfectada) AND " +
           "(:fechaInicio IS NULL OR a.fechaHora >= :fechaInicio) AND " +
           "(:fechaFin IS NULL OR a.fechaHora <= :fechaFin)")
    List<Auditoria> findByFiltrosOpcionales(
            @Param("entidadAfectada") String entidadAfectada,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);
}
