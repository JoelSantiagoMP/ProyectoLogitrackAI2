package com.example.logitrack.dto;

import com.example.logitrack.model.Auditoria;
import com.example.logitrack.model.TipoOperacion;

import java.time.LocalDateTime;

public class AuditoriaDTO {

    private Long id;
    private TipoOperacion tipoOperacion;
    private LocalDateTime fechaHora;
    private String usuario;
    private Long usuarioId;
    private String entidadAfectada;
    private Long entidadId;
    private String valorAnterior;
    private String valorNuevo;

    public static AuditoriaDTO from(Auditoria auditoria) {
        AuditoriaDTO dto = new AuditoriaDTO();
        dto.setId(auditoria.getId());
        dto.setTipoOperacion(auditoria.getTipoOperacion());
        dto.setFechaHora(auditoria.getFechaHora());
        if (auditoria.getUsuarioResponsable() != null) {
            dto.setUsuario(auditoria.getUsuarioResponsable().getUsername());
            dto.setUsuarioId(auditoria.getUsuarioResponsable().getId());
        }
        dto.setEntidadAfectada(auditoria.getEntidadAfectada());
        dto.setEntidadId(auditoria.getEntidadId());
        dto.setValorAnterior(auditoria.getValorAnterior());
        dto.setValorNuevo(auditoria.getValorNuevo());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoOperacion getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(TipoOperacion tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getEntidadAfectada() {
        return entidadAfectada;
    }

    public void setEntidadAfectada(String entidadAfectada) {
        this.entidadAfectada = entidadAfectada;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(Long entidadId) {
        this.entidadId = entidadId;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(String valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public void setValorNuevo(String valorNuevo) {
        this.valorNuevo = valorNuevo;
    }
}
