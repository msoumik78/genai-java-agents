package com.genai.agents.vo;

import java.math.BigDecimal;

public record OrderSnapshot(
        String orderId,
        String status,
        BigDecimal total,
        int daysSinceDelivery
) {
}
