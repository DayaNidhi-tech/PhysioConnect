package com.physioconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record AdminLocationRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Size(max = 255)
        String addressLine,

        @NotBlank
        @Size(max = 100)
        String city,

        @NotBlank
        @Size(max = 100)
        String state,

        @NotBlank
        @Size(max = 10)
        String pincode,

        @NotNull
        LocalTime openingTime,

        @NotNull
        LocalTime closingTime
) {
}
