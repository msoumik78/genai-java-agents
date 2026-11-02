package com.genai.agents.vo;

import java.math.BigDecimal;

public record Eligibility(
        String orderId,
        boolean eligible,
        BigDecimal maxRefund,
        String reason
) {
}
