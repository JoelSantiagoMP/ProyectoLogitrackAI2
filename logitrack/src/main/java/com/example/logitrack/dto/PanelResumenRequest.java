package com.example.logitrack.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public class PanelResumenRequest {

    @NotNull(message = "La fecha es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    @NotBlank(message = "La narrativa es obligatoria")
    @Size(min = 20, max = 500, message = "La narrativa debe tener entre 20 y 500 caracteres")
    private String narrativa;

    @NotNull(message = "Las alertas son obligatorias")
    private List<@Valid AlertaPanelRequest> alertas = new ArrayList<>();

    @NotNull(message = "Las acciones sugeridas son obligatorias")
    private List<@Valid AccionPanelRequest> accionesSugeridas = new ArrayList<>();

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getNarrativa() {
        return narrativa;
    }

    public void setNarrativa(String narrativa) {
        this.narrativa = narrativa;
    }

    public List<AlertaPanelRequest> getAlertas() {
        return alertas;
    }

    public void setAlertas(List<AlertaPanelRequest> alertas) {
        this.alertas = alertas;
    }

    public List<AccionPanelRequest> getAccionesSugeridas() {
        return accionesSugeridas;
    }

    public void setAccionesSugeridas(List<AccionPanelRequest> accionesSugeridas) {
        this.accionesSugeridas = accionesSugeridas;
    }
}
