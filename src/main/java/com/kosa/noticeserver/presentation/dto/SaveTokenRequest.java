package com.kosa.noticeserver.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveTokenRequest(
        @NotBlank String token
) {
}
