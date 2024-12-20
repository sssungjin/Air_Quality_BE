package com.sungjin.airquailitymonitordemo.service;

import com.sungjin.airquailitymonitordemo.dto.DeviceLocationDto;
import com.sungjin.airquailitymonitordemo.dto.request.device.DeviceLocationRequestDto;
import com.sungjin.airquailitymonitordemo.dto.request.device.DeviceRegistrationRequestDto;
import com.sungjin.airquailitymonitordemo.dto.response.device.DeviceLocationResponseDto;
import com.sungjin.airquailitymonitordemo.dto.response.device.DeviceRegistrationResponseDto;
import com.sungjin.airquailitymonitordemo.dto.response.device.DeviceResponseDto;
import com.sungjin.airquailitymonitordemo.entity.Device;
import com.sungjin.airquailitymonitordemo.entity.Project;
import com.sungjin.airquailitymonitordemo.repository.DeviceRepository;
import com.sungjin.airquailitymonitordemo.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final ProjectRepository projectRepository;

    public DeviceResponseDto getDevice(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found"));
        return convertToDto(device);
    }

    private DeviceResponseDto convertToDto(Device device) {
        // 위치 정보를 DTO로 변환
        DeviceLocationDto locationDto = device.getPlaceType() != null ?
                new DeviceLocationDto(
                        device.getFloorLevel(),
                        device.getPlaceType(),
                        device.getDescription()
                ) : null;

        return new DeviceResponseDto(
                device.getDeviceId(),
                device.getUserName(),
                device.getUserEmail(),
                device.getProject() != null ? device.getProject().getProjectId() : null,
                locationDto,
                device.getCreatedAt(),
                device.getUpdatedAt(),
                device.getTransmissionMode(),
                device.getUploadInterval()
        );
    }


    @Transactional
    public DeviceRegistrationResponseDto registerDevice(String deviceId, DeviceRegistrationRequestDto request) {
        log.info("Processing device registration/update for ID: {}", deviceId);

        boolean isNewDevice = !deviceRepository.existsById(deviceId);

        try {
            Project project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));

            Device device = deviceRepository.findById(deviceId)
                    .map(existingDevice -> {
                        log.info("Updating existing device: {}", deviceId);
                        existingDevice.setUserName(request.userName());
                        existingDevice.setUserEmail(request.userEmail());
                        existingDevice.setProject(project);
                        existingDevice.setTransmissionMode(request.transmissionMode());
                        existingDevice.setUploadInterval(request.uploadInterval());
                        return existingDevice;
                    })
                    .orElseGet(() -> {
                        log.info("Creating new device: {}", deviceId);
                        return Device.builder()
                                .deviceId(deviceId)
                                .userName(request.userName())
                                .userEmail(request.userEmail())
                                .project(project)
                                .transmissionMode(request.transmissionMode())
                                .uploadInterval(request.uploadInterval())
                                .build();
                    });

            deviceRepository.save(device);

            String message = isNewDevice
                    ? "Device registered successfully"
                    : "Device information updated successfully";

            log.info("Successfully {} device: {}", isNewDevice ? "registered" : "updated", deviceId);

            return new DeviceRegistrationResponseDto(true, message);

        } catch (EntityNotFoundException e) {
            log.error("Error processing device registration: {}", e.getMessage());
            return new DeviceRegistrationResponseDto(false, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during device registration", e);
            return new DeviceRegistrationResponseDto(false, "An unexpected error occurred");
        }
    }

    @Transactional
    public DeviceLocationResponseDto updateDeviceLocation(String deviceId, DeviceLocationRequestDto request) {
        log.info("Updating device location for ID: {}", deviceId);

        try {
            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new EntityNotFoundException("Device not found"));

            device.setFloorLevel(request.floorLevel());
            device.setPlaceType(request.placeType());
            device.setDescription(request.description());

            deviceRepository.save(device);

            DeviceLocationDto locationData = new DeviceLocationDto(
                    device.getFloorLevel(),
                    device.getPlaceType(),
                    device.getDescription()
            );

            return new DeviceLocationResponseDto(true, "Location updated successfully", locationData);

        } catch (EntityNotFoundException e) {
            log.error("Device not found: {}", deviceId);
            return new DeviceLocationResponseDto(false, e.getMessage(), null);
        } catch (Exception e) {
            log.error("Error updating device location", e);
            return new DeviceLocationResponseDto(false, "An unexpected error occurred", null);
        }
    }

    public DeviceLocationResponseDto getDeviceLocation(String deviceId) {
        log.info("Fetching device location for ID: {}", deviceId);

        try {
            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new EntityNotFoundException("Device not found"));

            DeviceLocationDto locationData = new DeviceLocationDto(
                    device.getFloorLevel(),
                    device.getPlaceType(),
                    device.getDescription()
            );

            return new DeviceLocationResponseDto(true, "Location retrieved successfully", locationData);

        } catch (EntityNotFoundException e) {
            log.error("Device not found: {}", deviceId);
            return new DeviceLocationResponseDto(false, e.getMessage(), null);
        } catch (Exception e) {
            log.error("Error fetching device location", e);
            return new DeviceLocationResponseDto(false, "An unexpected error occurred", null);
        }
    }
}