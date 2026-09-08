package com.physioconnect.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DoctorHolidayRequest(
        @NotNull
        @FutureOrPresent
        LocalDate holidayDate,

        @Size(max = 255)
        String reason
) {
}
