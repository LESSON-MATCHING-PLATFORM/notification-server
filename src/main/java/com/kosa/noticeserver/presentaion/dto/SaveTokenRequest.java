package com.kosa.noticeserver.presentaion.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveTokenRequest(
        @NotBlank String token,
        @NotBlank String userId
) {
}