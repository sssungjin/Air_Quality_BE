package com.sungjin.airquailitymonitordemo.dto.request.device;

import com.sungjin.airquailitymonitordemo.entity.enums.TransmissionMode;

public record DeviceRegistrationRequestDto(
        String deviceId,
        Long projectId,
//        String userName,
//        String userEmail,
        TransmissionMode transmissionMode,
        Integer uploadInterval
) {}
