package com.sungjin.airquailitymonitordemo.dto.request.device;

public record DeviceLocationRequestDto(
        Integer floorLevel,
        String placeType,
        String description
) {}