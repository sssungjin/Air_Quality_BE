package com.sungjin.airquailitymonitordemo.dto.request.user;

import com.sungjin.airquailitymonitordemo.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRegistrationRequestDto(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotNull UserRole role
) {}
