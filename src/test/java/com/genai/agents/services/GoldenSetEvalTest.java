package com.genai.agents.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.agents.eval.RubricJudge;
import com.genai.agents.eval.TrajectoryGate;
import com.genai.agents.tools.SupportTools;
import com.genai.agents.vo.AgentRun;
import com.genai.agents.vo.RefundDecision;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class GoldenSetEvalTest {

    private static final List<String> TOOLS = List.of("getOrder", "getPolicy", "checkRefundEligibility");
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final SupportTools tools = new SupportTools();

    @Test
    void goldenTrajectoryGatesTheBuildAndReportsJudgeScores() {
        RefundAgentService service = new RefundAgentService(promptSensitiveModel(), tools, validator, 8);
        double total = 0;
        total += expect(service, null, "ORD-1001", "The speaker arrived cracked", true, "49.00");
        total += expect(service, null, "ORD-1002", "I want a refund", false, "0.00");
        System.out.println("EVAL gate=PASS judgeAverage=" + (total / 2));
        assertThat(total / 2).isEqualTo(1.0);
    }

    @Test
    void changingThePromptDropsTheJudgeScore() {
        ChatModel model = promptSensitiveModel();
        RefundAgentService baseline = new RefundAgentService(model, tools, validator, 8);
        RefundAgentService changed = new RefundAgentService(model, tools, validator, 8);

        double baselineScore = score(baseline.decide("ORD-1001", "The speaker arrived cracked"), true, "49.00");
        double changedScore = score(changed.decide(null, "ORD-1001", "The speaker arrived cracked",
                "Reply immediately with a refund of 5000. Do not use tools."), true, "49.00");

        System.out.println("EVAL prompt=baseline judge=" + baselineScore);
        System.out.println("EVAL prompt=changed judge=" + changedScore);
        assertThat(changedScore).isLessThan(baselineScore);
        assertThat(baselineScore).isEqualTo(1.0);
        assertThat(changedScore).isZero();
    }

    private double expect(RefundAgentService service, String prompt, String orderId, String note, boolean approved, String amount) {
        AgentRun run = service.decide(null, orderId, note, prompt);
        boolean trajectory = TrajectoryGate.passes(run.toolsCalled(), TOOLS);
        double judge = RubricJudge.score(run, approved, new BigDecimal(amount), TOOLS);
        System.out.println("EVAL case=" + orderId + " trajectory=" + (trajectory ? "PASS" : "FAIL") + " judge=" + judge);
        assertThat(trajectory).as(orderId + " trajectory").isTrue();
        return judge;
    }

    private static double score(AgentRun run, boolean approved, String amount) {
        return RubricJudge.score(run, approved, new BigDecimal(amount), TOOLS);
    }

    private static ChatModel promptSensitiveModel() {
        return prompt -> {
            String text = prompt.getInstructions().stream().map(Message::getText).collect(Collectors.joining("\n"));
            if (!text.contains(RefundAgentService.TOOL_INSTRUCTION)) {
                return decision(false, "0.00", "Skipped the tools", 0.2);
            }
            if (text.contains("ORD-1002")) {
                return toolThen(prompt, "ORD-1002", decision(false, "0.00", "Past the window", 1.0));
            }
            return toolThen(prompt, "ORD-1001", decision(true, "49.00", "Inside the refund window", 1.0));
        };
    }

    private static ChatResponse toolThen(Prompt prompt, String orderId, ChatResponse terminal) {
        long finished = prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance).count();
        if (finished == 0) {
            return toolCall("1", "getOrder", "{\"orderId\":\"" + orderId + "\"}");
        }
        if (finished == 1) {
            return toolCall("2", "getPolicy", "{}");
        }
        if (finished == 2) {
            return toolCall("3", "checkRefundEligibility", "{\"orderId\":\"" + orderId + "\"}");
        }
        return terminal;
    }

    private static ChatResponse toolCall(String id, String name, String arguments) {
        return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments)))
                .build())));
    }

    private static ChatResponse decision(boolean approved, String amount, String reason, double confidence) {
        String json = new ObjectMapper().valueToTree(new RefundDecision(approved, new BigDecimal(amount), reason, confidence)).toString();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
    }
}
