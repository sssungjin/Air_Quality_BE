package com.sungjin.airquailitymonitordemo.dto.response.device;

import com.sungjin.airquailitymonitordemo.dto.DeviceLocationDto;
import com.sungjin.airquailitymonitordemo.entity.enums.TransmissionMode;

import java.time.LocalDateTime;

public record DeviceResponseDto(
        String deviceId,
//        String userName,
//        String userEmail,
        Long projectId,
        DeviceLocationDto location,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        TransmissionMode transmissionMode,
        Integer uploadInterval
) {}