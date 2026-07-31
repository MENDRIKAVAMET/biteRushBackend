package com.biterush.api.dto;

import com.biterush.api.entity.Role;

public class AuthResponseDTO {
    public String token;
    public String refreshToken;
    public Long id;
    public String username;
    public String name;
    public Role role;

    public AuthResponseDTO(String token) {
        this.token = token;
    }

    public AuthResponseDTO(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public AuthResponseDTO(String token, String refreshToken, Long id, String username, String name, Role role) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.name = name;
        this.role = role;
    }
}
