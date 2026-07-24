package com.example.logitrack.dto;

public class JwtAuthResponse {
    
    private String tokenDeAcceso;
    private String tipoDeToken = "Bearer";

    public JwtAuthResponse(String tokenDeAcceso) {
        this.tokenDeAcceso = tokenDeAcceso;
    }

    // Getters y Setters
    public String getTokenDeAcceso() {
        return tokenDeAcceso;
    }

    public void setTokenDeAcceso(String tokenDeAcceso) {
        this.tokenDeAcceso = tokenDeAcceso;
    }

    public String getTipoDeToken() {
        return tipoDeToken;
    }

    public void setTipoDeToken(String tipoDeToken) {
        this.tipoDeToken = tipoDeToken;
    }
}
