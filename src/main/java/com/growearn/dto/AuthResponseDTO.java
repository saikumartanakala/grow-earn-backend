package com.growearn.dto;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public class AuthResponseDTO {
    private String token;
    private String accessToken;
    private String refreshToken;
    private String role;

    public AuthResponseDTO() {}

    public AuthResponseDTO(String accessToken, String refreshToken, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.token = accessToken; // backward compatibility
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { 
        this.accessToken = accessToken; 
        this.token = accessToken;
    }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
