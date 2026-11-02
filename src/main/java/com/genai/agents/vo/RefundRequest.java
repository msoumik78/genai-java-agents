package com.genai.agents.vo;

import jakarta.validation.constraints.NotBlank;

public record RefundRequest(
        String threadId,
        @NotBlank String orderId,
        @NotBlank String customerNote
) {
}
