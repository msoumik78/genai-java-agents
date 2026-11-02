package com.genai.agents.services;

import com.genai.agents.store.InMemoryRefundLedger;
import com.genai.agents.store.InMemoryRunStore;
import com.genai.agents.store.JdbcDurableStore;
import com.genai.agents.tools.SupportTools;
import com.genai.agents.vo.ApprovalAction;
import com.genai.agents.vo.RunStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DurableRunTest {

    private final SupportTools tools = new SupportTools();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void resumesARunAfterTheProcessStopsMidLoop() {
        InMemoryRunStore store = new InMemoryRunStore();
        AtomicInteger calls = new AtomicInteger();
        ChatModel crashing = prompt -> {
            if (calls.incrementAndGet() == 1) {
                return toolCall("1", "getOrder", "{\"orderId\":\"ORD-1001\"}");
            }
            throw new IllegalStateException("killed");
        };
        var first = new RefundAgentService(crashing, tools, validator, store, new InMemoryRefundLedger(), 8);
        assertThatThrownBy(() -> first.decide("thread-1", "ORD-1001", "The speaker arrived cracked"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("killed");
        assertThat(store.find("thread-1").orElseThrow().status()).isEqualTo(RunStatus.RUNNING);

        ChatModel resumed = prompt -> jsonDecision(false, "0.00", "Changed my mind", 0.4);
        var second = new RefundAgentService(resumed, tools, validator, store, new InMemoryRefundLedger(), 8);
        var run = second.decide("thread-1", "ORD-1001", "The speaker arrived cracked");

        assertThat(run.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(run.decision().approved()).isFalse();
        assertThat(run.toolsCalled()).contains("getOrder");
        assertThat(run.modelCalls()).isGreaterThan(1);
    }

    @Test
    void pausesForApprovalAndIssuesTheRefundOnce() {
        InMemoryRunStore store = new InMemoryRunStore();
        InMemoryRefundLedger ledger = new InMemoryRefundLedger();
        ChatModel model = prompt -> jsonDecision(true, "49.00", "Inside the refund window", 0.9);
        var service = new RefundAgentService(model, tools, validator, store, ledger, 4);

        var paused = service.decide("thread-2", "ORD-1001", "Damaged");
        assertThat(paused.status()).isEqualTo(RunStatus.AWAITING_APPROVAL);
        assertThat(paused.receipt()).isNull();
        assertThat(ledger.findByKey("thread-2")).isEmpty();

        var edited = service.resolve("thread-2", ApprovalAction.EDIT, new BigDecimal("10.00"), "Partial refund");
        assertThat(edited.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(edited.receipt().amount()).isEqualByComparingTo("10.00");

        var again = service.resolve("thread-2", ApprovalAction.APPROVE, null, null);
        assertThat(again.receipt().amount()).isEqualByComparingTo("10.00");
        assertThat(ledger.findByKey("thread-2")).isPresent();
    }

    @Test
    void rejectDoesNotIssueARefund() {
        InMemoryRunStore store = new InMemoryRunStore();
        InMemoryRefundLedger ledger = new InMemoryRefundLedger();
        var service = new RefundAgentService(
                prompt -> jsonDecision(true, "49.00", "Inside the refund window", 0.9),
                tools, validator, store, ledger, 4);

        service.decide("thread-3", "ORD-1001", "Damaged");
        var rejected = service.resolve("thread-3", ApprovalAction.REJECT, null, null);

        assertThat(rejected.status()).isEqualTo(RunStatus.REJECTED);
        assertThat(rejected.decision().approved()).isFalse();
        assertThat(ledger.findByKey("thread-3")).isEmpty();
    }

    @Test
    void jdbcStoreSurvivesANewConnection() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:agent3;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        JdbcDurableStore first = new JdbcDurableStore(jdbc, new ObjectMapper());
        first.init();
        InMemoryRunStore unused = new InMemoryRunStore();
        var service = new RefundAgentService(
                prompt -> jsonDecision(true, "49.00", "Inside the refund window", 0.8),
                tools, validator, first, first, 4);
        service.decide("thread-db", "ORD-1001", "Damaged");

        JdbcDurableStore second = new JdbcDurableStore(jdbc, new ObjectMapper());
        var loaded = second.find("thread-db").orElseThrow();
        assertThat(loaded.status()).isEqualTo(RunStatus.AWAITING_APPROVAL);
        assertThat(loaded.orderId()).isEqualTo("ORD-1001");
        assertThat(unused.find("thread-db")).isEmpty();
    }

    private static ChatResponse toolCall(String id, String name, String arguments) {
        AssistantMessage assistant = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments)))
                .build();
        return new ChatResponse(List.of(new Generation(assistant)));
    }

    private static ChatResponse jsonDecision(boolean approved, String amount, String reason, double confidence) {
        String json = new ObjectMapper().valueToTree(new com.genai.agents.vo.RefundDecision(
                approved, new BigDecimal(amount), reason, confidence)).toString();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
    }
}
