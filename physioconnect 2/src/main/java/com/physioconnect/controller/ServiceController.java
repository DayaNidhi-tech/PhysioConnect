package com.physioconnect.controller;

import com.physioconnect.dto.AdminServiceRequest;
import com.physioconnect.dto.ApiResponse;
import com.physioconnect.dto.ServiceResponse;
import com.physioconnect.service.ServiceManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    public ServiceController(ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getActiveServices() {
        return ResponseEntity.ok(
                ApiResponse.success(serviceManagementService.getActiveServices())
        );
    }

    @PostMapping("/admin/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(
            @Valid @RequestBody AdminServiceRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(serviceManagementService.createService(request)));
    }

    @PutMapping("/admin/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(
            @PathVariable Long id,
            @Valid @RequestBody AdminServiceRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(serviceManagementService.updateService(id, request))
        );
    }
}
