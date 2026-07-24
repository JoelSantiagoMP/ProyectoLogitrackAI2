package com.example.logitrack.repository;

import com.example.logitrack.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    // Este "método mágico" le dice a Spring Boot que busque un usuario por su nombre
    Optional<Usuario> findByUsername(String username);
    
}