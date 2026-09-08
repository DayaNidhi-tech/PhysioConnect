package com.physioconnect.dto;

import com.physioconnect.entity.DoctorAvailability;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DoctorAvailabilityResponse(
        Long id,
        Long locationId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes
) {
    public static DoctorAvailabilityResponse from(DoctorAvailability availability) {
        return new DoctorAvailabilityResponse(
                availability.getId(),
                availability.getLocation().getId(),
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.getSlotDurationMinutes()
        );
    }
}
