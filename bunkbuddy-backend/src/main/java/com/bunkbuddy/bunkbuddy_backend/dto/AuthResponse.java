package com.bunkbuddy.bunkbuddy_backend.dto;
import lombok.Data;
import lombok.AllArgsConstructor;
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
}
