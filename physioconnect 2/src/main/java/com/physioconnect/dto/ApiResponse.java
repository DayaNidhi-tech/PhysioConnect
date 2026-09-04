package com.physioconnect.dto;

import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Map<String, Object> meta
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
                data,
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
                null,
                meta
        );
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return new ApiResponse<>(
                false,
                null,
                error,
                null
        );
    }
}
