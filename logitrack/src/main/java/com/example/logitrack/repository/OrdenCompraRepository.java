package com.example.logitrack.repository;

import com.example.logitrack.model.EstadoOrdenCompra;
import com.example.logitrack.model.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    List<OrdenCompra> findByEstado(EstadoOrdenCompra estado);
}
