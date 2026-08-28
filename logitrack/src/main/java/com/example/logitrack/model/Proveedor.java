package com.example.logitrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Column(nullable = false)
    private String nombre;

    @Column(name = "email")
    private String email;

    @NotNull(message = "Los días de entrega son obligatorios")
    @Min(value = 1, message = "Los días de entrega deben ser al menos 1")
    @Max(value = 90, message = "Los días de entrega no pueden superar 90")
    @Column(name = "dias_entrega", nullable = false)
    private Integer diasEntrega;
}
