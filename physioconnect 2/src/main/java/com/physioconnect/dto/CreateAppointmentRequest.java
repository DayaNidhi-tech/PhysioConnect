package com.physioconnect.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAppointmentRequest(
        @NotNull Long doctorId,
        @NotNull Long locationId,
        @NotNull Long serviceId,
        @NotNull Long slotId,
        @Size(max = 2000) String reasonForVisit,
        @Size(max = 5000) String medicalHistoryNotes
) {
}
