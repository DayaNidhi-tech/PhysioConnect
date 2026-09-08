package com.physioconnect.service;

import com.physioconnect.dto.DoctorBreakRequest;
import com.physioconnect.dto.DoctorBreakResponse;
import com.physioconnect.entity.Doctor;
import com.physioconnect.entity.DoctorBreakTime;
import com.physioconnect.exception.BadRequestException;
import com.physioconnect.exception.ResourceNotFoundException;
import com.physioconnect.repository.DoctorBreakTimeRepository;
import com.physioconnect.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DoctorBreakTimeService {

    private final DoctorBreakTimeRepository breakTimeRepository;
    private final DoctorRepository doctorRepository;

    public DoctorBreakTimeService(
            DoctorBreakTimeRepository breakTimeRepository,
            DoctorRepository doctorRepository
    ) {
        this.breakTimeRepository = breakTimeRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public List<DoctorBreakResponse> setBreakTimesByUserId(
            Long userId,
            DoctorBreakRequest request
    ) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        for (DoctorBreakRequest.BreakEntry entry : request.breaks()) {
            if (!entry.startTime().isBefore(entry.endTime())) {
                throw new BadRequestException("Break start time must be before end time");
            }
        }

        breakTimeRepository.deleteByDoctorId(doctor.getId());

        List<DoctorBreakTime> breaks = request.breaks().stream()
                .map(entry -> DoctorBreakTime.builder()
                        .doctor(doctor)
                        .dayOfWeek(entry.dayOfWeek())
                        .startTime(entry.startTime())
                        .endTime(entry.endTime())
                        .build())
                .toList();

        return breakTimeRepository.saveAll(breaks)
                .stream()
                .map(DoctorBreakResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DoctorBreakResponse> getBreakTimesByUserId(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        return breakTimeRepository.findByDoctorId(doctor.getId())
                .stream()
                .map(DoctorBreakResponse::from)
                .toList();
    }
}
