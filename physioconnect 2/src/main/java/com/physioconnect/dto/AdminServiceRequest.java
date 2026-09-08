package com.physioconnect.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdminServiceRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 10000)
        String description,

        @NotNull
        @Min(1)
        Integer defaultDurationMinutes,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal basePrice
) {
}
