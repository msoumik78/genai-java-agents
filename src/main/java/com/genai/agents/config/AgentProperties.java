package com.genai.agents.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "agent")
public record AgentProperties(
        @DefaultValue("8") int maxIterations,
        @DefaultValue("8000") int maxTotalTokens,
        @DefaultValue("3") int maxIdenticalToolCalls,
        @DefaultValue("200") BigDecimal maxRefundAmount,
        @DefaultValue("8") int keepRecentMessages,
        @DefaultValue("180") int toolResultMaxChars
) {
}
