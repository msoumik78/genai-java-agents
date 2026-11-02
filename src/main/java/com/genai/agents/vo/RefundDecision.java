package com.genai.agents.vo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RefundDecision(
        @NotNull Boolean approved,
        @NotNull @DecimalMin("0.00") BigDecimal amount,
        @NotBlank String reason,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double confidence
) {
}
