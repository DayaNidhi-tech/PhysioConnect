package com.physioconnect.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentOrderRequest(
        @NotNull(message = "Appointment ID is required")
        Long appointmentId
) {}
