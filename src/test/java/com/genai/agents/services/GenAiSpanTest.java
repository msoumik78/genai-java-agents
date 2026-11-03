package com.genai.agents.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.agents.tools.SupportTools;
import com.genai.agents.vo.RefundDecision;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenAiSpanTest {

    @Test
    void recordsChatAndToolSpansWithGenAiAttributes() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        var service = new RefundAgentService(
                prompt -> {
                    boolean toolReturned = prompt.getInstructions().stream().anyMatch(ToolResponseMessage.class::isInstance);
                    if (!toolReturned) {
                        return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                                .content("")
                                .toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "getOrder", "{\"orderId\":\"ORD-1001\"}")))
                                .build())));
                    }
                    String json = new ObjectMapper().valueToTree(
                            new RefundDecision(false, BigDecimal.ZERO, "No refund", 0.5)).toString();
                    return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
                },
                new SupportTools(),
                Validation.buildDefaultValidatorFactory().getValidator(),
                provider.get("com.genai.agents"),
                4);

        service.decide("ORD-1001", "Damaged");
        provider.forceFlush();

        List<String> names = exporter.getFinishedSpanItems().stream().map(SpanData::getName).toList();
        assertThat(names).contains("chat", "execute_tool");
        SpanData tool = exporter.getFinishedSpanItems().stream().filter(span -> span.getName().equals("execute_tool")).findFirst().orElseThrow();
        assertThat(tool.getAttributes().get(AttributeKey.stringKey("gen_ai.tool.name"))).isEqualTo("getOrder");
        assertThat(tool.getAttributes().get(AttributeKey.stringKey("gen_ai.operation.name"))).isEqualTo("execute_tool");
        SpanData chat = exporter.getFinishedSpanItems().stream().filter(span -> span.getName().equals("chat")).findFirst().orElseThrow();
        assertThat(chat.getAttributes().get(AttributeKey.stringKey("gen_ai.operation.name"))).isEqualTo("chat");
        assertThat(chat.getAttributes().get(AttributeKey.stringKey("gen_ai.system"))).isEqualTo("ollama");
        assertThat(chat.getAttributes().get(AttributeKey.stringKey("gen_ai.request.model"))).isEqualTo("qwen2.5:7b");
    }
}
