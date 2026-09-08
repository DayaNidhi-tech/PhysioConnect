package com.physioconnect.dto;

import java.math.BigDecimal;
import java.util.List;

public record DoctorResponse(
        Long id,
        UserResponse user,
        String specialization,
        String qualification,
        Integer experienceYears,
        BigDecimal consultationFee,
        String bio,
        BigDecimal averageRating,
        Boolean isApproved,
        List<Long> locationIds
) {
}