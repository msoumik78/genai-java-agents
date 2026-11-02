package com.genai.agents.client;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.concurrent.CompletableFuture;

public class ResilientChatModel implements ChatModel {

    private final ChatModel delegate;
    private final Retry retry;
    private final Bulkhead bulkhead;
    private final RateLimiter rateLimiter;
    private final TimeLimiter timeLimiter;

    public ResilientChatModel(
            ChatModel delegate,
            Retry retry,
            Bulkhead bulkhead,
            RateLimiter rateLimiter,
            TimeLimiter timeLimiter
    ) {
        this.delegate = delegate;
        this.retry = retry;
        this.bulkhead = bulkhead;
        this.rateLimiter = rateLimiter;
        this.timeLimiter = timeLimiter;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return rateLimiter.executeSupplier(() ->
                bulkhead.executeSupplier(() ->
                        retry.executeSupplier(() -> callWithTimeout(prompt))
                )
        );
    }

    private ChatResponse callWithTimeout(Prompt prompt) {
        try {
            return timeLimiter.executeFutureSupplier(
                    () -> CompletableFuture.supplyAsync(() -> delegate.call(prompt))
            );
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
