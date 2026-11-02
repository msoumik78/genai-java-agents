package com.genai.agents.vo;

import java.util.List;

public record AgentRun(
        String threadId,
        RunStatus status,
        RefundDecision decision,
        int modelCalls,
        int schemaRepairs,
        List<String> toolsCalled,
        List<RailEvent> railsFired,
        int totalTokens,
        RefundReceipt receipt
) {
}
