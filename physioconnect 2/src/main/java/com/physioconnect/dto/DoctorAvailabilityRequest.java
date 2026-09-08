package com.physioconnect.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record DoctorAvailabilityRequest(
        @NotEmpty
        List<@Valid AvailabilityEntry> availability
) {
    public record AvailabilityEntry(
            @NotNull
            Long locationId,

            @NotNull
            DayOfWeek dayOfWeek,

            @NotNull
            LocalTime startTime,

            @NotNull
            LocalTime endTime,

            @NotNull
            Integer slotDurationMinutes
    ) {
    }
}
