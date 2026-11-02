package com.genai.agents.hooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.agents.config.AgentProperties;
import com.genai.agents.exception.RailBlockedException;
import com.genai.agents.vo.RailEvent;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
@Order(50)
public class ToolGuardrail implements AgentHook {

    private static final Set<String> ALLOWED = Set.of("getOrder", "getPolicy", "checkRefundEligibility");

    private final BigDecimal maxRefundAmount;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ToolGuardrail(AgentProperties properties) {
        this(properties.maxRefundAmount());
    }

    public ToolGuardrail(BigDecimal maxRefundAmount) {
        this.maxRefundAmount = maxRefundAmount;
    }

    @Override
    public void beforeTool(HarnessState state, AssistantMessage.ToolCall call) {
        if (!ALLOWED.contains(call.name())) {
            block(call.name(), "tool-not-allowed", "Tool is not on the allowlist: " + call.name());
        }
        JsonNode args = parse(call.arguments());
        if (args.hasNonNull("orderId") && !state.orderId().equals(args.get("orderId").asText())) {
            block(call.name(), "order-out-of-scope",
                    "Tool may only read order " + state.orderId() + ", not " + args.get("orderId").asText());
        }
        if (args.has("amount") && args.get("amount").isNumber()
                && args.get("amount").decimalValue().compareTo(maxRefundAmount) > 0) {
            block(call.name(), "amount-over-limit",
                    "Tool argument amount exceeds the auto-approve limit of " + maxRefundAmount);
        }
    }

    private JsonNode parse(String arguments) {
        try {
            if (arguments == null || arguments.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(arguments);
        } catch (Exception ex) {
            block("arguments", "invalid-arguments", "Tool arguments are not valid JSON");
            return objectMapper.createObjectNode();
        }
    }

    private static void block(String tool, String code, String detail) {
        throw new RailBlockedException(new RailEvent("tool", code, tool + ": " + detail));
    }
}
