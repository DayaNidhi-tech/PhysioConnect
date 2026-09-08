package com.physioconnect.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAppointmentStatusRequest(
        @NotBlank(message = "Status is required")
        String status
) {
}
