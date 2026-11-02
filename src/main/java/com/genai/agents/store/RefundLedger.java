package com.genai.agents.store;

import com.genai.agents.vo.RefundReceipt;

import java.math.BigDecimal;
import java.util.Optional;

public interface RefundLedger {

    RefundReceipt issue(String idempotencyKey, String threadId, String orderId, BigDecimal amount);

    Optional<RefundReceipt> findByKey(String idempotencyKey);
}
