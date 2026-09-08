package com.physioconnect.controller.admin;

import com.physioconnect.dto.AdminDoctorCreateRequest;
import com.physioconnect.dto.ApiResponse;
import com.physioconnect.service.AdminDoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/doctors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorController {

    private final AdminDoctorService adminDoctorService;

    public AdminDoctorController(
            AdminDoctorService adminDoctorService
    ) {
        this.adminDoctorService = adminDoctorService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createDoctor(
            @Valid @RequestBody AdminDoctorCreateRequest request
    ) {
        adminDoctorService.createDoctor(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(null));
    }
}
