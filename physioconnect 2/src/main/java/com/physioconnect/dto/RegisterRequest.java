package com.physioconnect.dto;

import com.physioconnect.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-registration payload. Callers are NOT trusted to pick their own role
 * beyond the two self-service roles; ADMIN can only be assigned server-side.
 */
public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "A valid email address is required")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotBlank(message = "Phone is required")
        @Size(min = 7, max = 20, message = "Phone must be between 7 and 20 characters")
        String phone,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        Role role
) {
    /** Self-service registration only allows patient/doctor roles. */
    public Role effectiveRole() {
        return role == null ? Role.PATIENT : role;
    }
}
