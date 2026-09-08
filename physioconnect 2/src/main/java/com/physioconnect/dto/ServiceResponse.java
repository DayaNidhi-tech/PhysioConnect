package com.physioconnect.dto;

import com.physioconnect.entity.PhysioService;

import java.math.BigDecimal;

public record ServiceResponse(
        Long id,
        String name,
        String description,
        Integer defaultDurationMinutes,
        BigDecimal basePrice,
        Boolean isActive
) {
    public static ServiceResponse from(PhysioService service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getDefaultDurationMinutes(),
                service.getBasePrice(),
                service.getIsActive()
        );
    }
}
