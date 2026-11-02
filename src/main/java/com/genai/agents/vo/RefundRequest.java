package com.genai.agents.vo;

import jakarta.validation.constraints.NotBlank;

public record RefundRequest(
        @NotBlank String orderId,
        @NotBlank String customerNote
) {
}
