package com.genai.agents.vo;

import java.util.List;

public record AgentRun(
        RefundDecision decision,
        int modelCalls,
        int schemaRepairs,
        List<String> toolsCalled
) {
}
