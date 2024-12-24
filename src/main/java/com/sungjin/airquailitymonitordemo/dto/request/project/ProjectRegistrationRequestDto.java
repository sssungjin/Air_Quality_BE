package com.sungjin.airquailitymonitordemo.dto.request.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectRegistrationRequestDto(
        @NotBlank String nationCode,
        @NotBlank String title,
        @NotBlank String description,
        @NotNull String startDate,
        @NotNull String endDate,
        @NotBlank String collectionMethod, // 실시간, 파일업로드
        @NotBlank String participantEmail, // 참여자 이메일
        @NotBlank String participantPhone, // 참여자 전화번호
        @NotBlank String participantBirth, // 참여자 생년월일
        @NotBlank String participantGender, // 참여자 성별
        @NotNull boolean termOfUse, // 이용약관 동의
        @NotNull String additionalConsent // 추가 동의
) {} 