package com.example.logitrack.repository;

import com.example.logitrack.model.ResumenPanel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ResumenPanelRepository extends JpaRepository<ResumenPanel, Long> {

    Optional<ResumenPanel> findByFecha(LocalDate fecha);

    Optional<ResumenPanel> findFirstByOrderByIdDesc();
}
