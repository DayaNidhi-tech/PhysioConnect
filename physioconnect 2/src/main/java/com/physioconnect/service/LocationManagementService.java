package com.physioconnect.service;

import com.physioconnect.dto.AdminLocationRequest;
import com.physioconnect.dto.LocationResponse;
import com.physioconnect.entity.Location;
import com.physioconnect.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocationManagementService {

    private final LocationRepository locationRepository;

    public LocationManagementService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getActiveLocations() {
        return locationRepository.findByIsActiveTrue()
                .stream()
                .map(LocationResponse::from)
                .toList();
    }

    @Transactional
    public LocationResponse createLocation(AdminLocationRequest request) {
        Location location = Location.builder()
                .name(request.name().trim())
                .addressLine(request.addressLine().trim())
                .city(request.city().trim())
                .state(request.state().trim())
                .pincode(request.pincode().trim())
                .openingTime(request.openingTime())
                .closingTime(request.closingTime())
                .isActive(true)
                .build();

        return LocationResponse.from(locationRepository.save(location));
    }
}
