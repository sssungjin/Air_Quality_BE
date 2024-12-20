package com.sungjin.airquailitymonitordemo.dto.response.sensordata;

public record SensorDataUploadResponseDto(
        boolean success,
        String message,
        int processedCount
) {}
