package com.genai.agents.store;

import com.genai.agents.vo.RefundReceipt;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRefundLedger implements RefundLedger {

    private final ConcurrentHashMap<String, RefundReceipt> issued = new ConcurrentHashMap<>();

    @Override
    public RefundReceipt issue(String idempotencyKey, String threadId, String orderId, BigDecimal amount) {
        return issued.computeIfAbsent(idempotencyKey, key -> new RefundReceipt(key, orderId, amount));
    }

    @Override
    public Optional<RefundReceipt> findByKey(String idempotencyKey) {
        return Optional.ofNullable(issued.get(idempotencyKey));
    }
}
