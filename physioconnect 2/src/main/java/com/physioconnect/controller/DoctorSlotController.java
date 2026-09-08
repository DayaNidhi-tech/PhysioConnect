package com.physioconnect.controller;

import com.physioconnect.dto.ApiResponse;
import com.physioconnect.dto.TimeSlotResponse;
import com.physioconnect.service.DoctorSlotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorSlotController {

    private final DoctorSlotService doctorSlotService;

    public DoctorSlotController(DoctorSlotService doctorSlotService) {
        this.doctorSlotService = doctorSlotService;
    }

    @GetMapping("/{doctorId}/slots")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getAvailableSlots(
            @PathVariable Long doctorId,
            @RequestParam LocalDate date,
            @RequestParam Long locationId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        doctorSlotService.getAvailableSlots(doctorId, date, locationId)
                )
        );
    }
}
