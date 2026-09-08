package com.physioconnect.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

public record AdminDoctorUpdateRequest(

        @NotBlank
        @Size(max = 150)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @NotBlank
        @Size(max = 20)
        String phone,

        @NotBlank
        @Size(max = 150)
        String specialization,

        @NotBlank
        @Size(max = 255)
        String qualification,

        @NotNull
        @Min(0)
        Integer experienceYears,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal consultationFee,

        @Size(max = 10000)
        String bio,

        @NotEmpty
        Set<Long> locationIds
) {
}
