package com.physioconnect.controller;

import com.physioconnect.dto.ApiResponse;
import com.physioconnect.dto.DoctorBreakRequest;
import com.physioconnect.dto.DoctorBreakResponse;
import com.physioconnect.security.UserPrincipal;
import com.physioconnect.service.DoctorBreakTimeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors/me/breaks")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorBreakTimeController {

    private final DoctorBreakTimeService doctorBreakTimeService;

    public DoctorBreakTimeController(DoctorBreakTimeService doctorBreakTimeService) {
        this.doctorBreakTimeService = doctorBreakTimeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorBreakResponse>>> getBreakTimes(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        doctorBreakTimeService.getBreakTimesByUserId(principal.getId())
                )
        );
    }

    @PutMapping
    public ResponseEntity<ApiResponse<List<DoctorBreakResponse>>> setBreakTimes(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DoctorBreakRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        doctorBreakTimeService.setBreakTimesByUserId(
                                principal.getId(),
                                request
                        )
                )
        );
    }
}
