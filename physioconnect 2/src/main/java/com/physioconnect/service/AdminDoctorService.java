package com.physioconnect.service;

import com.physioconnect.dto.AdminDoctorCreateRequest;
import com.physioconnect.dto.AdminDoctorStatusRequest;
import com.physioconnect.dto.AdminDoctorUpdateRequest;
import com.physioconnect.dto.DoctorResponse;
import com.physioconnect.dto.UserResponse;
import com.physioconnect.entity.Doctor;
import com.physioconnect.entity.Location;
import com.physioconnect.entity.User;
import com.physioconnect.entity.enums.Role;
import com.physioconnect.exception.BadRequestException;
import com.physioconnect.exception.ResourceNotFoundException;
import com.physioconnect.repository.DoctorRepository;
import com.physioconnect.repository.LocationRepository;
import com.physioconnect.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminDoctorService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDoctorService(
            UserRepository userRepository,
            DoctorRepository doctorRepository,
            LocationRepository locationRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.locationRepository = locationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public DoctorResponse createDoctor(AdminDoctorCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        String phone = request.phone().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }

        if (userRepository.existsByPhone(phone)) {
            throw new BadRequestException("An account with this phone number already exists");
        }

        Set<Long> locationIds = request.locationIds();
        Set<Location> locations = loadActiveLocations(locationIds);

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.DOCTOR)
                .isActive(false)
                .isEmailVerified(false)
                .build();

        user = userRepository.save(user);

        Doctor doctor = Doctor.builder()
                .user(user)
                .specialization(request.specialization().trim())
                .qualification(request.qualification().trim())
                .experienceYears(request.experienceYears())
                .consultationFee(request.consultationFee())
                .bio(request.bio())
                .isApproved(false)
                .locations(locations)
                .build();

        return toDoctorResponse(doctorRepository.save(doctor));
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll()
                .stream()
                .map(this::toDoctorResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(Long doctorId) {
        return toDoctorResponse(findDoctor(doctorId));
    }

    @Transactional
    public DoctorResponse updateDoctor(
            Long doctorId,
            AdminDoctorUpdateRequest request
    ) {
        Doctor doctor = findDoctor(doctorId);
        User user = doctor.getUser();

        String email = request.email().trim().toLowerCase();
        String phone = request.phone().trim();

        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }

        if (!phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
            throw new BadRequestException("An account with this phone number already exists");
        }

        Set<Location> locations = loadActiveLocations(request.locationIds());

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(phone);

        doctor.setSpecialization(request.specialization().trim());
        doctor.setQualification(request.qualification().trim());
        doctor.setExperienceYears(request.experienceYears());
        doctor.setConsultationFee(request.consultationFee());
        doctor.setBio(request.bio());
        doctor.setLocations(locations);

        return toDoctorResponse(doctorRepository.save(doctor));
    }

    @Transactional
    public DoctorResponse updateStatus(
            Long doctorId,
            AdminDoctorStatusRequest request
    ) {
        Doctor doctor = findDoctor(doctorId);
        boolean active = Boolean.TRUE.equals(request.active());

        doctor.getUser().setIsActive(active);
        doctor.setIsApproved(active);

        return toDoctorResponse(doctorRepository.save(doctor));
    }

    private Doctor findDoctor(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found with id: " + doctorId
                ));
    }

    private Set<Location> loadActiveLocations(Set<Long> locationIds) {
        if (locationIds == null || locationIds.isEmpty()) {
            throw new BadRequestException("At least one service location is required");
        }

        List<Long> locationIdList = new ArrayList<>(locationIds);
        List<Location> locations = locationRepository.findAllById(locationIdList);

        if (locations.size() != locationIds.size()) {
            throw new ResourceNotFoundException(
                    "One or more service locations were not found"
            );
        }

        Set<Long> inactiveLocationIds = locations.stream()
                .filter(location -> !Boolean.TRUE.equals(location.getIsActive()))
                .map(Location::getId)
                .collect(Collectors.toSet());

        if (!inactiveLocationIds.isEmpty()) {
            throw new BadRequestException(
                    "Inactive service locations cannot be assigned to a Doctor: "
                            + inactiveLocationIds
            );
        }

        return new HashSet<>(locations);
    }

    private DoctorResponse toDoctorResponse(Doctor doctor) {
        List<Long> locationIds = doctor.getLocations() == null
                ? List.of()
                : doctor.getLocations()
                        .stream()
                        .map(Location::getId)
                        .sorted()
                        .toList();

        return new DoctorResponse(
                doctor.getId(),
                UserResponse.from(doctor.getUser()),
                doctor.getSpecialization(),
                doctor.getQualification(),
                doctor.getExperienceYears(),
                doctor.getConsultationFee(),
                doctor.getBio(),
                doctor.getAverageRating(),
                doctor.getIsApproved(),
                locationIds
        );
    }
}
