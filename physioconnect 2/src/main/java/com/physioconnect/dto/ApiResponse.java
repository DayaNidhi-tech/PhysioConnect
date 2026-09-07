package com.physioconnect.dto;

import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        ApiError error,
        Map<String, Object> meta
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
                data,
                "Request successful",
                null,
                null
        );
    }

    public static <T> ApiResponse<T> success(
            T data,
            Map<String, Object> meta
    ) {
        return new ApiResponse<>(
                true,
                data,
                "Request successful",
                null,
                meta
        );
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return new ApiResponse<>(
                false,
                null,
                null,
                error,
                null
        );
    }
}
