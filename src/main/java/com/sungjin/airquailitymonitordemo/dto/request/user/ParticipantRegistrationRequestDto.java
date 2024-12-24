package com.sungjin.airquailitymonitordemo.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParticipantRegistrationRequestDto(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotNull boolean termOfUse,
        @NotNull boolean additionalConsent
) {} 