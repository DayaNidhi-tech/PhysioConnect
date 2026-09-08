package com.physioconnect.service;

import com.physioconnect.dto.DoctorAvailabilityRequest;
import com.physioconnect.dto.DoctorAvailabilityResponse;
import com.physioconnect.entity.Doctor;
import com.physioconnect.entity.DoctorAvailability;
import com.physioconnect.entity.Location;
import com.physioconnect.exception.BadRequestException;
import com.physioconnect.exception.ResourceNotFoundException;
import com.physioconnect.repository.DoctorAvailabilityRepository;
import com.physioconnect.repository.DoctorRepository;
import com.physioconnect.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DoctorAvailabilityService {

    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;
    private final LocationRepository locationRepository;

    public DoctorAvailabilityService(
            DoctorAvailabilityRepository availabilityRepository,
            DoctorRepository doctorRepository,
            LocationRepository locationRepository
    ) {
        this.availabilityRepository = availabilityRepository;
        this.doctorRepository = doctorRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public List<DoctorAvailabilityResponse> setWeeklyAvailability(
            Long doctorId,
            DoctorAvailabilityRequest request
    ) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        Set<Long> locationIds = new HashSet<>();
        for (DoctorAvailabilityRequest.AvailabilityEntry entry : request.availability()) {
            validateEntry(entry);
            locationIds.add(entry.locationId());
        }

        List<Location> locations = locationRepository.findAllById(locationIds);
        if (locations.size() != locationIds.size()) {
            throw new ResourceNotFoundException("One or more service locations were not found");
        }

        for (Location location : locations) {
            if (!Boolean.TRUE.equals(location.getIsActive())) {
                throw new BadRequestException("Only active service locations can be used for availability");
            }
            if (doctor.getLocations() == null || !doctor.getLocations().contains(location)) {
                throw new BadRequestException("Doctor is not assigned to the selected service location");
            }
        }

        availabilityRepository.deleteByDoctorId(doctorId);

        List<DoctorAvailability> availability = request.availability().stream()
                .map(entry -> DoctorAvailability.builder()
                        .doctor(doctor)
                        .location(locations.stream()
                                .filter(location -> location.getId().equals(entry.locationId()))
                                .findFirst()
                                .orElseThrow())
                        .dayOfWeek(entry.dayOfWeek())
                        .startTime(entry.startTime())
                        .endTime(entry.endTime())
                        .slotDurationMinutes(entry.slotDurationMinutes())
                        .build())
                .toList();

        return availabilityRepository.saveAll(availability)
                .stream()
                .map(DoctorAvailabilityResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DoctorAvailabilityResponse> getWeeklyAvailability(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor not found");
        }

        return availabilityRepository.findByDoctorId(doctorId)
                .stream()
                .map(DoctorAvailabilityResponse::from)
                .toList();
    }

    private void validateEntry(DoctorAvailabilityRequest.AvailabilityEntry entry) {
        if (!entry.startTime().isBefore(entry.endTime())) {
            throw new BadRequestException("Availability start time must be before end time");
        }

        if (entry.slotDurationMinutes() == null || entry.slotDurationMinutes() <= 0) {
            throw new BadRequestException("Slot duration must be greater than zero");
        }
    }
}
