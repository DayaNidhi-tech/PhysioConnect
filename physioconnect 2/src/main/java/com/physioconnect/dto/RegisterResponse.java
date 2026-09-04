package com.physioconnect.dto;

public record RegisterResponse(
        Long userId,
        String email,
        String role,
        String accessToken,
        boolean refreshTokenSet
) {
}