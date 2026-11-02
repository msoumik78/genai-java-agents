package com.genai.agents.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "llm")
public record LlmClientProperties(
        @DefaultValue("5") int permitsPerSecond,
        @DefaultValue("2") int maxConcurrentCalls,
        @DefaultValue("90s") Duration timeout,
        @DefaultValue("2") int retryAttempts
) {
}
