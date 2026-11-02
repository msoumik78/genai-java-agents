package com.genai.agents.hooks;

import com.genai.agents.config.AgentProperties;
import com.genai.agents.exception.RailBlockedException;
import com.genai.agents.vo.RailEvent;
import com.genai.agents.vo.RefundDecision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@Component
@Order(60)
public class OutputGuardrail implements AgentHook {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private final BigDecimal maxRefundAmount;

    @Autowired
    public OutputGuardrail(AgentProperties properties) {
        this(properties.maxRefundAmount());
    }

    public OutputGuardrail(BigDecimal maxRefundAmount) {
        this.maxRefundAmount = maxRefundAmount;
    }

    @Override
    public void afterDecision(HarnessState state, RefundDecision decision) {
        if (decision.amount().compareTo(maxRefundAmount) > 0) {
            block("amount-over-limit",
                    "Proposed amount " + decision.amount() + " exceeds the auto-approve limit of " + maxRefundAmount);
        }
        if (decision.reason() != null && EMAIL.matcher(decision.reason()).find()) {
            block("pii-in-output", "Decision reason contains an email address");
        }
    }

    private static void block(String code, String detail) {
        throw new RailBlockedException(new RailEvent("output", code, detail));
    }
}
