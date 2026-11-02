package com.genai.agents.services;

import com.genai.agents.config.AgentProperties;
import com.genai.agents.exception.LoopLimitExceededException;
import com.genai.agents.tools.SupportTools;
import com.genai.agents.vo.AgentRun;
import com.genai.agents.vo.RefundDecision;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RefundAgentService {

    private final ChatModel chatModel;
    private final SupportTools supportTools;
    private final Validator validator;
    private final int maxIterations;
    private final ToolCallingManager toolCallingManager;

    @Autowired
    public RefundAgentService(ChatModel chatModel, SupportTools supportTools, Validator validator, AgentProperties properties) {
        this(chatModel, supportTools, validator, properties.maxIterations(), ToolCallingManager.builder().build());
    }

    RefundAgentService(ChatModel chatModel, SupportTools supportTools, Validator validator, int maxIterations) {
        this(chatModel, supportTools, validator, maxIterations, ToolCallingManager.builder().build());
    }

    private RefundAgentService(
            ChatModel chatModel,
            SupportTools supportTools,
            Validator validator,
            int maxIterations,
            ToolCallingManager toolCallingManager
    ) {
        if (maxIterations < 1) {
            throw new IllegalArgumentException("maxIterations must be at least 1");
        }
        this.chatModel = chatModel;
        this.supportTools = supportTools;
        this.validator = validator;
        this.maxIterations = maxIterations;
        this.toolCallingManager = toolCallingManager;
    }

    public AgentRun decide(String orderId, String customerNote) {
        BeanOutputConverter<RefundDecision> converter = new BeanOutputConverter<>(RefundDecision.class);
        ToolCallback[] tools = MethodToolCallbackProvider.builder().toolObjects(supportTools).build().getToolCallbacks();
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(tools)
                .internalToolExecutionEnabled(false)
                .build();

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("""
                You are a refund support agent. You propose a decision. You do not issue the refund.
                Use getOrder, getPolicy, and checkRefundEligibility before you decide.
                When you are ready, reply with JSON only, matching this schema:
                %s
                """.formatted(converter.getFormat())));
        messages.add(new UserMessage("Order %s. Customer says: %s".formatted(orderId, customerNote)));

        int modelCalls = 0;
        int schemaRepairs = 0;
        while (true) {
            if (modelCalls >= maxIterations) {
                throw new LoopLimitExceededException(maxIterations);
            }
            modelCalls++;
            Prompt prompt = new Prompt(List.copyOf(messages), options);
            ChatResponse response = chatModel.call(prompt);
            if (response.hasToolCalls()) {
                ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response);
                messages = new ArrayList<>(result.conversationHistory());
                continue;
            }

            String text = response.getResult().getOutput().getText();
            try {
                RefundDecision decision = requireValid(converter.convert(text));
                return new AgentRun(decision, modelCalls, schemaRepairs, toolNames(messages));
            } catch (RuntimeException ex) {
                schemaRepairs++;
                messages.add(assistantText(text));
                messages.add(new UserMessage(
                        "The decision was rejected: " + ex.getMessage()
                                + ". Reply with JSON only, matching the schema."
                ));
            }
        }
    }

    private RefundDecision requireValid(RefundDecision decision) {
        if (decision == null) {
            throw new IllegalArgumentException("empty decision");
        }
        Set<ConstraintViolation<RefundDecision>> violations = validator.validate(decision);
        if (violations.isEmpty()) {
            return decision;
        }
        String details = violations.stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining("; "));
        throw new IllegalArgumentException(details);
    }

    private static AssistantMessage assistantText(String text) {
        return new AssistantMessage(text == null ? "" : text);
    }

    private static List<String> toolNames(List<Message> messages) {
        return messages.stream()
                .filter(AssistantMessage.class::isInstance)
                .map(AssistantMessage.class::cast)
                .flatMap(message -> message.getToolCalls().stream())
                .map(AssistantMessage.ToolCall::name)
                .toList();
    }
}
