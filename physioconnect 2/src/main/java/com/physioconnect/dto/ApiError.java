package com.physioconnect.dto;

public record ApiError(
        String code,
        String message
) {
}