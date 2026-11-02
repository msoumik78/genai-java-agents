package com.genai.agents.config;

import com.genai.agents.client.ResilientChatModel;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
public class LlmClientConfig {

    @Bean
    Retry llmRetry(LlmClientProperties properties) {
        return Retry.of("llm", RetryConfig.custom()
                .maxAttempts(properties.retryAttempts())
                .waitDuration(Duration.ofMillis(200))
                .retryExceptions(java.io.IOException.class, java.util.concurrent.TimeoutException.class)
                .build());
    }

    @Bean
    Bulkhead llmBulkhead(LlmClientProperties properties) {
        return Bulkhead.of("llm", BulkheadConfig.custom()
                .maxConcurrentCalls(properties.maxConcurrentCalls())
                .maxWaitDuration(Duration.ofSeconds(1))
                .build());
    }

    @Bean
    RateLimiter llmRateLimiter(LlmClientProperties properties) {
        return RateLimiter.of("llm", RateLimiterConfig.custom()
                .limitForPeriod(properties.permitsPerSecond())
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(2))
                .build());
    }

    @Bean
    TimeLimiter llmTimeLimiter(LlmClientProperties properties) {
        return TimeLimiter.of("llm", TimeLimiterConfig.custom()
                .timeoutDuration(properties.timeout())
                .cancelRunningFuture(true)
                .build());
    }

    @Bean
    @Primary
    ChatModel resilientChatModel(
            OllamaChatModel ollamaChatModel,
            Retry llmRetry,
            Bulkhead llmBulkhead,
            RateLimiter llmRateLimiter,
            TimeLimiter llmTimeLimiter
    ) {
        return new ResilientChatModel(ollamaChatModel, llmRetry, llmBulkhead, llmRateLimiter, llmTimeLimiter);
    }
}
