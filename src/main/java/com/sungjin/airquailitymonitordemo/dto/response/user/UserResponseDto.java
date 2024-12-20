package com.sungjin.airquailitymonitordemo.dto.response.user;

import com.sungjin.airquailitymonitordemo.entity.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String email,
        String name,
        UserRole role,
        String kibanaAccessKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
