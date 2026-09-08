package com.physioconnect.dto;

import com.physioconnect.entity.Location;

import java.time.LocalTime;

public record LocationResponse(
        Long id,
        String name,
        String addressLine,
        String city,
        String state,
        String pincode,
        LocalTime openingTime,
        LocalTime closingTime,
        Boolean isActive
) {
    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getAddressLine(),
                location.getCity(),
                location.getState(),
                location.getPincode(),
                location.getOpeningTime(),
                location.getClosingTime(),
                location.getIsActive()
        );
    }
}
