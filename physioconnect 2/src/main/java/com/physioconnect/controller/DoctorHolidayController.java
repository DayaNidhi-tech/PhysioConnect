package com.physioconnect.controller;

import com.physioconnect.dto.ApiResponse;
import com.physioconnect.dto.DoctorHolidayRequest;
import com.physioconnect.dto.DoctorHolidayResponse;
import com.physioconnect.security.UserPrincipal;
import com.physioconnect.service.DoctorHolidayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors/me/holidays")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorHolidayController {

    private final DoctorHolidayService doctorHolidayService;

    public DoctorHolidayController(DoctorHolidayService doctorHolidayService) {
        this.doctorHolidayService = doctorHolidayService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorHolidayResponse>> addHoliday(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DoctorHolidayRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        doctorHolidayService.addHoliday(principal.getId(), request)
                ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorHolidayResponse>>> getHolidays(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(doctorHolidayService.getHolidays(principal.getId()))
        );
    }
}
