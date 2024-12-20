package com.sungjin.airquailitymonitordemo.dto.response.project;

import java.util.List;

public record ProjectListResponseDto(
        List<ProjectResponseDto> projects
) {}