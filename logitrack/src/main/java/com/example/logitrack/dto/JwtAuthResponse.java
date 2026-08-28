package com.example.logitrack.dto;

public class JwtAuthResponse {

    private String token;
    private String accessToken;
    private String tokenType = "Bearer";
    private String username;
    private String rol;

    public JwtAuthResponse(String token) {
        this.token = token;
        this.accessToken = token;
    }

    public JwtAuthResponse(String token, String username, String rol) {
        this.token = token;
        this.accessToken = token;
        this.username = username;
        this.rol = rol;
    }

    public JwtAuthResponse(String token, String tokenType) {
        this.token = token;
        this.accessToken = token;
        this.tokenType = tokenType;
    }

    // Getters y Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        this.accessToken = token;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        this.token = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}