package com.genai.agents.vo;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ApprovalRequest(
        @NotNull ApprovalAction action,
        BigDecimal amount,
        String reason
) {
}
