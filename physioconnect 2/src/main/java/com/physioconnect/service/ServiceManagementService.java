package com.physioconnect.service;

import com.physioconnect.dto.AdminServiceRequest;
import com.physioconnect.dto.ServiceResponse;
import com.physioconnect.entity.PhysioService;
import com.physioconnect.exception.ResourceNotFoundException;
import com.physioconnect.repository.PhysioServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceManagementService {

    private final PhysioServiceRepository physioServiceRepository;

    public ServiceManagementService(PhysioServiceRepository physioServiceRepository) {
        this.physioServiceRepository = physioServiceRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> getActiveServices() {
        return physioServiceRepository.findByIsActiveTrue()
                .stream()
                .map(ServiceResponse::from)
                .toList();
    }

    @Transactional
    public ServiceResponse createService(AdminServiceRequest request) {
        PhysioService service = PhysioService.builder()
                .name(request.name().trim())
                .description(request.description())
                .defaultDurationMinutes(request.defaultDurationMinutes())
                .basePrice(request.basePrice())
                .isActive(true)
                .build();

        return ServiceResponse.from(physioServiceRepository.save(service));
    }

    @Transactional
    public ServiceResponse updateService(Long id, AdminServiceRequest request) {
        PhysioService service = physioServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        service.setName(request.name().trim());
        service.setDescription(request.description());
        service.setDefaultDurationMinutes(request.defaultDurationMinutes());
        service.setBasePrice(request.basePrice());

        return ServiceResponse.from(physioServiceRepository.save(service));
    }
}
