package com.physioconnect.dto;

import jakarta.validation.constraints.NotNull;

public record AdminDoctorStatusRequest(
        @NotNull
        Boolean active
) {
}
