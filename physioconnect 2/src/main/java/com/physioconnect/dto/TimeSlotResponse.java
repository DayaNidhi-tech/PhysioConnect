package com.physioconnect.dto;

import com.physioconnect.entity.TimeSlot;
import com.physioconnect.entity.enums.SlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record TimeSlotResponse(
        Long id,
        Long doctorId,
        Long locationId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        SlotStatus status
) {
    public static TimeSlotResponse from(TimeSlot slot) {
        return new TimeSlotResponse(
                slot.getId(),
                slot.getDoctor().getId(),
                slot.getLocation().getId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus()
        );
    }
}
