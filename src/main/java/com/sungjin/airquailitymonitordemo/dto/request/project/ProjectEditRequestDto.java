package com.sungjin.airquailitymonitordemo.dto.request.project;

import jakarta.validation.constraints.NotBlank;

public record ProjectEditRequestDto(
        @NotBlank String projectName,
        @NotBlank String description,
        @NotBlank String collectionMethod
) {}