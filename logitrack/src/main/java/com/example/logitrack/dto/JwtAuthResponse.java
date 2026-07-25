package com.example.logitrack.dto;

public class JwtAuthResponse {

    private String token;
    private String accessToken;
    private String tokenType = "Bearer";

    public JwtAuthResponse(String token) {
        this.token = token;
        this.accessToken = token;
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
}