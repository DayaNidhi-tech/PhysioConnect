package com.physioconnect.controller;

import com.physioconnect.dto.AdminLocationRequest;
import com.physioconnect.dto.ApiResponse;
import com.physioconnect.dto.LocationResponse;
import com.physioconnect.service.LocationManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class LocationController {

    private final LocationManagementService locationManagementService;

    public LocationController(LocationManagementService locationManagementService) {
        this.locationManagementService = locationManagementService;
    }

    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getActiveLocations() {
        return ResponseEntity.ok(
                ApiResponse.success(locationManagementService.getActiveLocations())
        );
    }

    @PostMapping("/admin/locations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LocationResponse>> createLocation(
            @Valid @RequestBody AdminLocationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(locationManagementService.createLocation(request)));
    }
}
