package com.genai.agents.vo;

import java.math.BigDecimal;

public record RefundReceipt(String idempotencyKey, String orderId, BigDecimal amount) {
}
