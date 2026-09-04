package com.physioconnect.dto;

public record AuthResponse(
        String accessToken,
        String role,
        Profile profile
) {

    public record Profile(
            Long id,
            String fullName,
            String email
    ) {
    }
}
