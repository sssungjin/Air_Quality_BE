package com.sungjin.airquailitymonitordemo.controller;

import com.sungjin.airquailitymonitordemo.dto.request.device.DeviceLocationRequestDto;
import com.sungjin.airquailitymonitordemo.dto.request.device.DeviceRegistrationRequestDto;
import com.sungjin.airquailitymonitordemo.dto.response.device.DeviceLocationResponseDto;
import com.sungjin.airquailitymonitordemo.dto.response.device.DeviceRegistrationResponseDto;
import com.sungjin.airquailitymonitordemo.dto.response.device.DeviceResponseDto;
import com.sungjin.airquailitymonitordemo.entity.SensorData;
import com.sungjin.airquailitymonitordemo.service.DeviceService;
import com.sungjin.airquailitymonitordemo.service.SensorDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.sungjin.airquailitymonitordemo.security.CustomUserPrincipal;

@RestController
@RequestMapping("/api/v1/devices")
@Slf4j
@RequiredArgsConstructor
public class DeviceController {

    private final SensorDataService sensorDataService;
    private final DeviceService deviceService;

    @GetMapping("/{deviceId}/latest")
    public ResponseEntity<SensorData> getLatestData(
            @PathVariable String deviceId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        try {
            SensorData latestData = sensorDataService.getLatestSensorData(deviceId);
            if (latestData == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(latestData);
        } catch (Exception e) {
            log.error("Error fetching latest data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceResponseDto> getDevice(@PathVariable String deviceId) {
        try {
            DeviceResponseDto device = deviceService.getDevice(deviceId);
            return ResponseEntity.ok(device);
        } catch (Exception e) {
            log.error("Error getting device info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{deviceId}/register")
    public ResponseEntity<DeviceRegistrationResponseDto> registerDevice(
            @PathVariable String deviceId,
            @RequestBody DeviceRegistrationRequestDto request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        try {
            DeviceRegistrationResponseDto response = deviceService.registerDevice(deviceId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error registering device", e);
            return ResponseEntity.ok(new DeviceRegistrationResponseDto(false, e.getMessage()));
        }
    }

    @PutMapping("/{deviceId}/location")
    public ResponseEntity<DeviceLocationResponseDto> updateDeviceLocationInfo(
            @PathVariable String deviceId,
            @RequestBody DeviceLocationRequestDto request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        log.info("Received location update request for device {}: {}", deviceId, request);
        try {
            DeviceLocationResponseDto response = deviceService.updateDeviceLocation(deviceId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating device location", e);
            return ResponseEntity.ok(new DeviceLocationResponseDto(false, e.getMessage(), null));
        }
    }

    @GetMapping("/{deviceId}/location")
    public ResponseEntity<DeviceLocationResponseDto> getDeviceLocationInfo(
            @PathVariable String deviceId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        log.info("Received location request for device: {}", deviceId);
        try {
            DeviceLocationResponseDto response = deviceService.getDeviceLocation(deviceId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching device location", e);
            return ResponseEntity.ok(new DeviceLocationResponseDto(false, e.getMessage(), null));
        }
    }
}
