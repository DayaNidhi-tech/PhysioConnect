package com.physioconnect.dto;

import jakarta.validation.constraints.NotNull;

public record RescheduleAppointmentRequest(
        @NotNull(message = "Slot ID is required")
        Long slotId
) {
}
