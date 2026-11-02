package com.genai.agents.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent")
public record AgentProperties(int maxIterations) {
}
