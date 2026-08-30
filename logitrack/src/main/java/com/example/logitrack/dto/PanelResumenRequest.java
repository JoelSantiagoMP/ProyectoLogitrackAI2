package com.example.logitrack.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(description = "Contrato estricto del resumen del panel (POST/GET /api/panel/resumen)")
public class PanelResumenRequest {

    @Schema(description = "Fecha actual en America/Bogota (YYYY-MM-DD)", example = "2026-08-24")

    @NotNull(message = "La fecha es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    @Schema(description = "Texto entre 20 y 500 caracteres",
            example = "Hay productos en riesgo y una orden pendiente de aprobación.")

    @NotBlank(message = "La narrativa es obligatoria")
    @Size(min = 20, max = 500, message = "La narrativa debe tener entre 20 y 500 caracteres")
    private String narrativa;

    @Schema(description = "Alertas con severidad BAJA, MEDIA o ALTA; al menos un ID por alerta")

    @NotNull(message = "Las alertas son obligatorias")
    private List<@Valid AlertaPanelRequest> alertas = new ArrayList<>();

    @Schema(description = "Acciones REVISAR_ORDEN, REVISAR_PRODUCTO o REVISAR_BODEGA; exactamente un ID por acción")
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
