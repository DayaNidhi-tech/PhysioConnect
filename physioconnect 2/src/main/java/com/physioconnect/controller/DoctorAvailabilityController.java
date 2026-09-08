package com.physioconnect.controller;

import com.physioconnect.dto.ApiResponse;
import com.physioconnect.dto.DoctorAvailabilityRequest;
import com.physioconnect.dto.DoctorAvailabilityResponse;
import com.physioconnect.security.UserPrincipal;
import com.physioconnect.service.DoctorAvailabilityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors/me/availability")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorAvailabilityController {

    private final DoctorAvailabilityService doctorAvailabilityService;

    public DoctorAvailabilityController(DoctorAvailabilityService doctorAvailabilityService) {
        this.doctorAvailabilityService = doctorAvailabilityService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorAvailabilityResponse>>> getWeeklyAvailability(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        doctorAvailabilityService.getWeeklyAvailabilityByUserId(principal.getId())
                )
        );
    }

    @PutMapping
    public ResponseEntity<ApiResponse<List<DoctorAvailabilityResponse>>> setWeeklyAvailability(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DoctorAvailabilityRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        doctorAvailabilityService.setWeeklyAvailabilityByUserId(
                                principal.getId(),
                                request
                        )
                )
        );
    }
}
