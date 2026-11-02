package com.genai.agents.services;

import com.genai.agents.exception.LoopLimitExceededException;
import com.genai.agents.tools.SupportTools;
import com.genai.agents.vo.AgentRun;
import com.genai.agents.vo.RefundDecision;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundAgentServiceTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final SupportTools tools = new SupportTools();

    @Test
    void executesToolsThenAcceptsAValidDecision() {
        List<Prompt> prompts = new ArrayList<>();
        ChatModel model = scripted(prompts,
                toolCall("1", "getOrder", "{\"orderId\":\"ORD-1001\"}"),
                toolCall("2", "checkRefundEligibility", "{\"orderId\":\"ORD-1001\"}"),
                jsonDecision(true, "49.00", "Delivered 12 days ago and inside the window", 0.9)
        );

        AgentRun run = new RefundAgentService(model, tools, validator, 8).decide("ORD-1001", "The speaker arrived cracked");

        assertThat(run.decision().approved()).isTrue();
        assertThat(run.decision().amount()).isEqualByComparingTo("49.00");
        assertThat(run.modelCalls()).isEqualTo(3);
        assertThat(run.schemaRepairs()).isZero();
        assertThat(run.toolsCalled()).containsExactly("getOrder", "checkRefundEligibility");
        assertThat(prompts.get(1).getInstructions())
                .anyMatch(message -> message instanceof ToolResponseMessage);
        String toolResult = prompts.get(1).getInstructions().stream()
                .filter(ToolResponseMessage.class::isInstance)
                .map(ToolResponseMessage.class::cast)
                .map(Object::toString)
                .findFirst()
                .orElseThrow();
        assertThat(toolResult).contains("ORD-1001").contains("49.00");
    }

    @Test
    void sendsSchemaErrorsBackAndAcceptsTheRepairedDecision() {
        ChatModel model = scripted(new ArrayList<>(),
                jsonDecision(true, "49.00", "ok", 2.0),
                jsonDecision(true, "49.00", "Inside the refund window", 0.8)
        );

        AgentRun run = new RefundAgentService(model, tools, validator, 4).decide("ORD-1001", "Damaged");

        assertThat(run.schemaRepairs()).isEqualTo(1);
        assertThat(run.decision().confidence()).isEqualTo(0.8);
        assertThat(run.modelCalls()).isEqualTo(2);
    }

    @Test
    void stopsWhenTheModelNeverFinishes() {
        ChatModel model = scripted(new ArrayList<>(),
                toolCall("1", "getPolicy", "{}"),
                toolCall("2", "getPolicy", "{}"),
                toolCall("3", "getPolicy", "{}")
        );

        assertThatThrownBy(() -> new RefundAgentService(model, tools, validator, 2).decide("ORD-1001", "Where is my refund?"))
                .isInstanceOf(LoopLimitExceededException.class)
                .hasMessageContaining("2");
    }

    @Test
    void returnsAToolFailureToTheModelInsteadOfCrashingTheLoop() {
        List<Prompt> prompts = new ArrayList<>();
        ChatModel model = scripted(prompts,
                toolCall("1", "getOrder", "{\"orderId\":\"ORD-404\"}"),
                jsonDecision(false, "0.00", "Order does not exist", 0.95)
        );

        AgentRun run = new RefundAgentService(model, tools, validator, 4).decide("ORD-404", "Refund please");

        assertThat(run.decision().approved()).isFalse();
        assertThat(prompts.get(1).getInstructions())
                .anySatisfy(message -> assertThat(message.toString()).contains("Unknown order ORD-404"));
    }

    private static ChatModel scripted(List<Prompt> seen, ChatResponse... responses) {
        AtomicInteger index = new AtomicInteger();
        return prompt -> {
            seen.add(prompt);
            int current = index.getAndIncrement();
            if (current >= responses.length) {
                throw new IllegalStateException("script ended");
            }
            return responses[current];
        };
    }

    private static ChatResponse toolCall(String id, String name, String arguments) {
        AssistantMessage assistant = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments)))
                .build();
        return new ChatResponse(List.of(new Generation(assistant)));
    }

    private static ChatResponse jsonDecision(boolean approved, String amount, String reason, double confidence) {
        String json = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(new RefundDecision(
                approved,
                new BigDecimal(amount),
                reason,
                confidence
        )).toString();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
    }
}
