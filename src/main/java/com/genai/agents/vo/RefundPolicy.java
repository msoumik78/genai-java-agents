package com.genai.agents.vo;

import java.math.BigDecimal;

public record RefundPolicy(
        int windowDays,
        BigDecimal autoApproveLimit,
        String summary
) {
}
