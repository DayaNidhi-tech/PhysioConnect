package com.physioconnect.dto;

public record TokenResponse(
        String accessToken,
        String role,
        AuthResponse.Profile profile
) {
}
