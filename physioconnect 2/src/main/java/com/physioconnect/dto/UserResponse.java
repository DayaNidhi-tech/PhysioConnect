package com.physioconnect.dto;

import com.physioconnect.entity.User;
import com.physioconnect.entity.enums.Role;

/**
 * Safe projection of a user. Never contains the password hash.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        Role role,
        boolean emailVerified
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                Boolean.TRUE.equals(user.getIsEmailVerified())
        );
    }
}
