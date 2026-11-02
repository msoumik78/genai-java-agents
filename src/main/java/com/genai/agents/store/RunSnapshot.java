package com.genai.agents.store;

import com.genai.agents.vo.RailEvent;
import com.genai.agents.vo.RefundDecision;
import com.genai.agents.vo.RefundReceipt;
import com.genai.agents.vo.RunStatus;

import java.math.BigDecimal;
import java.util.List;

public record RunSnapshot(
        String threadId,
        String orderId,
        String customerNote,
        RunStatus status,
        List<StoredMessage> messages,
        int modelCalls,
        int schemaRepairs,
        List<RailEvent> rails,
        int totalTokens,
        RefundDecision decision,
        BigDecimal pendingAmount,
        String pendingReason,
        RefundReceipt receipt,
        List<String> pinnedFacts
) {
}
