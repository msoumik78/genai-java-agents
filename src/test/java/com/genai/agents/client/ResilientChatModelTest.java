package com.genai.agents.client;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilientChatModelTest {

    @Test
    void retriesATransientFailureThenReturnsTheModelResponse() {
        AtomicInteger calls = new AtomicInteger();
        ChatResponse ok = new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
        var model = client(
                prompt -> {
                    if (calls.incrementAndGet() == 1) {
                        throw new IllegalStateException(new java.util.concurrent.TimeoutException("slow"));
                    }
                    return ok;
                },
                2,
                Duration.ofSeconds(5),
                5
        );

        assertThat(model.call(new Prompt("hello")).getResult().getOutput().getText()).isEqualTo("ok");
        assertThat(calls).hasValue(2);
    }

    @Test
    void rateLimiterRejectsASecondCallInTheSameWindow() {
        var model = client(prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))), 1, Duration.ofSeconds(5), 1);

        model.call(new Prompt("one"));
        assertThatThrownBy(() -> model.call(new Prompt("two")))
                .isInstanceOf(RequestNotPermitted.class);
    }

    private static ResilientChatModel client(
            org.springframework.ai.chat.model.ChatModel delegate,
            int attempts,
            Duration timeout,
            int permitsPerSecond
    ) {
        Retry retry = Retry.of("llm", RetryConfig.custom()
                .maxAttempts(attempts)
                .waitDuration(Duration.ofMillis(1))
                .retryExceptions(IllegalStateException.class)
                .build());
        Bulkhead bulkhead = Bulkhead.of("llm", BulkheadConfig.custom()
                .maxConcurrentCalls(2)
                .maxWaitDuration(Duration.ofMillis(50))
                .build());
        RateLimiter rateLimiter = RateLimiter.of("llm", RateLimiterConfig.custom()
                .limitForPeriod(permitsPerSecond)
                .limitRefreshPeriod(Duration.ofSeconds(10))
                .timeoutDuration(Duration.ZERO)
                .build());
        TimeLimiter timeLimiter = TimeLimiter.of("llm", TimeLimiterConfig.custom()
                .timeoutDuration(timeout)
                .cancelRunningFuture(true)
                .build());
        return new ResilientChatModel(delegate, retry, bulkhead, rateLimiter, timeLimiter);
    }
}
