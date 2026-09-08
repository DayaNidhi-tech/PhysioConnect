package com.physioconnect.service;

import com.physioconnect.dto.DoctorHolidayRequest;
import com.physioconnect.dto.DoctorHolidayResponse;
import com.physioconnect.entity.Doctor;
import com.physioconnect.entity.DoctorHoliday;
import com.physioconnect.exception.BadRequestException;
import com.physioconnect.exception.ResourceNotFoundException;
import com.physioconnect.repository.DoctorHolidayRepository;
import com.physioconnect.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DoctorHolidayService {

    private final DoctorHolidayRepository holidayRepository;
    private final DoctorRepository doctorRepository;

    public DoctorHolidayService(
            DoctorHolidayRepository holidayRepository,
            DoctorRepository doctorRepository
    ) {
        this.holidayRepository = holidayRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public DoctorHolidayResponse addHoliday(Long userId, DoctorHolidayRequest request) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        if (!holidayRepository.findByDoctorIdAndHolidayDate(doctor.getId(), request.holidayDate()).isEmpty()) {
            throw new BadRequestException("Holiday already exists for the selected date");
        }

        DoctorHoliday holiday = DoctorHoliday.builder()
                .doctor(doctor)
                .holidayDate(request.holidayDate())
                .reason(request.reason())
                .build();

        return DoctorHolidayResponse.from(holidayRepository.save(holiday));
    }

    @Transactional(readOnly = true)
    public List<DoctorHolidayResponse> getHolidays(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        return holidayRepository.findByDoctorIdOrderByHolidayDateAsc(doctor.getId())
                .stream()
                .map(DoctorHolidayResponse::from)
                .toList();
    }
}
