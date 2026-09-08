package com.physioconnect.dto;

import com.physioconnect.entity.DoctorBreakTime;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DoctorBreakResponse(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
    public static DoctorBreakResponse from(DoctorBreakTime breakTime) {
        return new DoctorBreakResponse(
                breakTime.getId(),
                breakTime.getDayOfWeek(),
                breakTime.getStartTime(),
                breakTime.getEndTime()
        );
    }
}
