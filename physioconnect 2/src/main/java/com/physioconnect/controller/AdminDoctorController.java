package com.physioconnect.controller.admin;

import com.physioconnect.dto.AdminDoctorCreateRequest;
import com.physioconnect.dto.AdminDoctorStatusRequest;
import com.physioconnect.dto.AdminDoctorUpdateRequest;
import com.physioconnect.dto.ApiResponse;
import com.physioconnect.dto.DoctorResponse;
import com.physioconnect.service.AdminDoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/doctors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorController {

    private final AdminDoctorService adminDoctorService;

    public AdminDoctorController(AdminDoctorService adminDoctorService) {
        this.adminDoctorService = adminDoctorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAllDoctors() {
        return ResponseEntity.ok(
                ApiResponse.success(adminDoctorService.getAllDoctors())
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorResponse>> createDoctor(
            @Valid @RequestBody AdminDoctorCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminDoctorService.createDoctor(request)));
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(
            @PathVariable Long doctorId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(adminDoctorService.getDoctorById(doctorId))
        );
    }

    @PutMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(
            @PathVariable Long doctorId,
            @Valid @RequestBody AdminDoctorUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        adminDoctorService.updateDoctor(doctorId, request)
                )
        );
    }

    @PatchMapping("/{doctorId}/status")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateStatus(
            @PathVariable Long doctorId,
            @Valid @RequestBody AdminDoctorStatusRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        adminDoctorService.updateStatus(doctorId, request)
                )
        );
    }
}
