package com.physioconnect.dto;

import com.physioconnect.entity.DoctorHoliday;

import java.time.LocalDate;

public record DoctorHolidayResponse(
        Long id,
        LocalDate holidayDate,
        String reason
) {
    public static DoctorHolidayResponse from(DoctorHoliday holiday) {
        return new DoctorHolidayResponse(
                holiday.getId(),
                holiday.getHolidayDate(),
                holiday.getReason()
        );
    }
}
