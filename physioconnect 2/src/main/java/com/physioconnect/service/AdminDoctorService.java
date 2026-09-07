package com.physioconnect.service;

import com.physioconnect.dto.AdminDoctorCreateRequest;
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
    public Doctor createDoctor(AdminDoctorCreateRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase();

        String phone = request.phone()
                .trim();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException(
                    "An account with this email already exists"
            );
        }

        if (userRepository.existsByPhone(phone)) {
            throw new BadRequestException(
                    "An account with this phone number already exists"
            );
        }

        Set<Long> locationIds = request.locationIds();

        if (locationIds == null || locationIds.isEmpty()) {
            throw new BadRequestException(
                    "At least one service location is required"
            );
        }

        List<Long> locationIdList = new ArrayList<>(locationIds);

        List<Location> locations =
                locationRepository.findAllById(locationIdList);

        if (locations.size() != locationIds.size()) {
            throw new ResourceNotFoundException(
                    "One or more service locations were not found"
            );
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phone(phone)
                .passwordHash(
                        passwordEncoder.encode(request.password())
                )
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
                .locations(new HashSet<>(locations))
                .build();

        return doctorRepository.save(doctor);
    }
}
