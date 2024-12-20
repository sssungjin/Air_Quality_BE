package com.sungjin.airquailitymonitordemo.dto.response.device;

import com.sungjin.airquailitymonitordemo.dto.DeviceLocationDto;

public record DeviceLocationResponseDto(
        boolean success,
        String message,
        DeviceLocationDto data
) {}