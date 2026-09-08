package com.physioconnect.service;

import com.physioconnect.dto.TimeSlotResponse;
import com.physioconnect.entity.Doctor;
import com.physioconnect.entity.DoctorAvailability;
import com.physioconnect.entity.DoctorBreakTime;
import com.physioconnect.entity.Location;
import com.physioconnect.entity.TimeSlot;
import com.physioconnect.entity.enums.SlotStatus;
import com.physioconnect.exception.BadRequestException;
import com.physioconnect.exception.ResourceNotFoundException;
import com.physioconnect.repository.DoctorAvailabilityRepository;
import com.physioconnect.repository.DoctorBreakTimeRepository;
import com.physioconnect.repository.DoctorHolidayRepository;
import com.physioconnect.repository.DoctorRepository;
import com.physioconnect.repository.LocationRepository;
import com.physioconnect.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DoctorSlotService {

    private final DoctorRepository doctorRepository;
    private final LocationRepository locationRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorBreakTimeRepository breakTimeRepository;
    private final DoctorHolidayRepository holidayRepository;
    private final TimeSlotRepository timeSlotRepository;

    public DoctorSlotService(
            DoctorRepository doctorRepository,
            LocationRepository locationRepository,
            DoctorAvailabilityRepository availabilityRepository,
            DoctorBreakTimeRepository breakTimeRepository,
            DoctorHolidayRepository holidayRepository,
            TimeSlotRepository timeSlotRepository
    ) {
        this.doctorRepository = doctorRepository;
        this.locationRepository = locationRepository;
        this.availabilityRepository = availabilityRepository;
        this.breakTimeRepository = breakTimeRepository;
        this.holidayRepository = holidayRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional
    public List<TimeSlotResponse> getAvailableSlots(
            Long doctorId,
            LocalDate date,
            Long locationId
    ) {
        if (date == null) {
            throw new BadRequestException("Date is required");
        }
        if (locationId == null) {
            throw new BadRequestException("Location ID is required");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (!Boolean.TRUE.equals(doctor.getUser().getIsActive())
                || !Boolean.TRUE.equals(doctor.getIsApproved())) {
            return List.of();
        }

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Service location not found"));

        if (!Boolean.TRUE.equals(location.getIsActive())) {
            return List.of();
        }

        if (doctor.getLocations() == null || !doctor.getLocations().contains(location)) {
            throw new BadRequestException("Doctor is not assigned to the selected service location");
        }

        if (holidayRepository.findByDoctorIdAndHolidayDate(doctorId, date).size() > 0) {
            return List.of();
        }

        List<TimeSlot> existingSlots = timeSlotRepository
                .findByDoctorIdAndLocationIdAndSlotDateOrderByStartTimeAsc(doctorId, locationId, date);

        Set<String> existingStarts = new HashSet<>();
        for (TimeSlot slot : existingSlots) {
            existingStarts.add(slot.getStartTime().toString());
        }

        List<DoctorAvailability> availabilities = availabilityRepository
                .findByDoctorIdAndDayOfWeek(doctorId, date.getDayOfWeek())
                .stream()
                .filter(a -> a.getLocation().getId().equals(locationId))
                .toList();

        if (availabilities.isEmpty()) {
            return List.of();
        }

        List<DoctorBreakTime> breaks = breakTimeRepository.findByDoctorId(doctorId)
                .stream()
                .filter(b -> b.getDayOfWeek() == date.getDayOfWeek())
                .toList();

        List<TimeSlot> generated = new ArrayList<>();

        for (DoctorAvailability availability : availabilities) {
            int duration = availability.getSlotDurationMinutes();
            LocalTime start = availability.getStartTime();

            while (!start.plusMinutes(duration).isAfter(availability.getEndTime())) {
                LocalTime end = start.plusMinutes(duration);

                if (!overlapsBreak(start, end, breaks)
                        && !existingStarts.contains(start.toString())) {
                    TimeSlot slot = TimeSlot.builder()
                            .doctor(doctor)
                            .location(location)
                            .slotDate(date)
                            .startTime(start)
                            .endTime(end)
                            .status(SlotStatus.AVAILABLE)
                            .build();
                    generated.add(slot);
                    existingStarts.add(start.toString());
                }

                start = end;
            }
        }

        if (!generated.isEmpty()) {
            timeSlotRepository.saveAll(generated);
        }

        return timeSlotRepository
                .findByDoctorIdAndLocationIdAndSlotDateOrderByStartTimeAsc(doctorId, locationId, date)
                .stream()
                .filter(this::isBookable)
                .map(TimeSlotResponse::from)
                .sorted(Comparator.comparing(TimeSlotResponse::startTime))
                .toList();
    }

    private boolean overlapsBreak(LocalTime slotStart, LocalTime slotEnd, List<DoctorBreakTime> breaks) {
        for (DoctorBreakTime breakTime : breaks) {
            if (slotStart.isBefore(breakTime.getEndTime())
                    && slotEnd.isAfter(breakTime.getStartTime())) {
                return true;
            }
        }
        return false;
    }

    private boolean isBookable(TimeSlot slot) {
        if (slot.getStatus() == SlotStatus.AVAILABLE) {
            return true;
        }

        if (slot.getStatus() == SlotStatus.HELD
                && slot.getHeldUntil() != null
                && slot.getHeldUntil().isBefore(java.time.LocalDateTime.now())) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slot.setHeldUntil(null);
            timeSlotRepository.save(slot);
            return true;
        }

        return false;
    }
}
