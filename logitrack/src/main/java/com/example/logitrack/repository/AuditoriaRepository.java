package com.example.logitrack.repository;

import com.example.logitrack.model.Auditoria;
import com.example.logitrack.model.TipoOperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("SELECT a FROM Auditoria a JOIN FETCH a.usuarioResponsable ORDER BY a.fechaHora DESC")
    List<Auditoria> findAllWithUsuario();

    @Query("SELECT a FROM Auditoria a JOIN FETCH a.usuarioResponsable WHERE a.id = :id")
    Optional<Auditoria> findByIdWithUsuario(@Param("id") Long id);

    @Query("SELECT a FROM Auditoria a JOIN FETCH a.usuarioResponsable WHERE LOWER(a.usuarioResponsable.username) = :username ORDER BY a.fechaHora DESC")
    List<Auditoria> findByUsuarioResponsableUsernameIgnoreCase(@Param("username") String username);

    @Query("SELECT a FROM Auditoria a JOIN FETCH a.usuarioResponsable WHERE a.tipoOperacion = :tipoOperacion ORDER BY a.fechaHora DESC")
    List<Auditoria> findByTipoOperacion(@Param("tipoOperacion") TipoOperacion tipoOperacion);

    @Query("SELECT a FROM Auditoria a JOIN FETCH a.usuarioResponsable WHERE LOWER(a.entidadAfectada) = :entidadAfectada ORDER BY a.fechaHora DESC")
    List<Auditoria> findByEntidadAfectadaIgnoreCase(@Param("entidadAfectada") String entidadAfectada);

    @Query("""
            SELECT a FROM Auditoria a JOIN FETCH a.usuarioResponsable
            WHERE (:filtrarEntidad = false OR LOWER(a.entidadAfectada) = :entidadAfectada)
              AND (:filtrarFechaInicio = false OR a.fechaHora >= :fechaInicio)
              AND (:filtrarFechaFin = false OR a.fechaHora <= :fechaFin)
            ORDER BY a.fechaHora DESC
            """)
    List<Auditoria> findByFiltrosOpcionales(
            @Param("filtrarEntidad") boolean filtrarEntidad,
            @Param("entidadAfectada") String entidadAfectada,
            @Param("filtrarFechaInicio") boolean filtrarFechaInicio,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("filtrarFechaFin") boolean filtrarFechaFin,
            @Param("fechaFin") LocalDateTime fechaFin);
}