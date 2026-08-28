package com.example.logitrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProveedorDTO {

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    private String nombre;

    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotNull(message = "Los días de entrega son obligatorios")
    @Min(value = 1, message = "Los días de entrega deben ser al menos 1")
    @Max(value = 90, message = "Los días de entrega no pueden superar 90")
    private Integer diasEntrega;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getDiasEntrega() {
        return diasEntrega;
    }

    public void setDiasEntrega(Integer diasEntrega) {
        this.diasEntrega = diasEntrega;
    }
}
