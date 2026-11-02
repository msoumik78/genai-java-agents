package com.genai.agents.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.agents.vo.RefundReceipt;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Optional;

public class JdbcDurableStore implements RunStore, RefundLedger {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcDurableStore(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void init() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_run (
                    thread_id VARCHAR(64) PRIMARY KEY,
                    body CLOB NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS refund_ledger (
                    idempotency_key VARCHAR(128) PRIMARY KEY,
                    thread_id VARCHAR(64) NOT NULL,
                    order_id VARCHAR(64) NOT NULL,
                    amount DECIMAL(12, 2) NOT NULL
                )
                """);
    }

    @Override
    public Optional<RunSnapshot> find(String threadId) {
        var rows = jdbc.query("SELECT body FROM agent_run WHERE thread_id = ?", (rs, n) -> rs.getString("body"), threadId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(read(rows.getFirst(), RunSnapshot.class));
    }

    @Override
    public void save(RunSnapshot snapshot) {
        String body = write(snapshot);
        int updated = jdbc.update("UPDATE agent_run SET body = ? WHERE thread_id = ?", body, snapshot.threadId());
        if (updated == 0) {
            jdbc.update("INSERT INTO agent_run (thread_id, body) VALUES (?, ?)", snapshot.threadId(), body);
        }
    }

    @Override
    public RefundReceipt issue(String idempotencyKey, String threadId, String orderId, BigDecimal amount) {
        var existing = findByKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        jdbc.update(
                "INSERT INTO refund_ledger (idempotency_key, thread_id, order_id, amount) VALUES (?, ?, ?, ?)",
                idempotencyKey, threadId, orderId, amount
        );
        return new RefundReceipt(idempotencyKey, orderId, amount);
    }

    @Override
    public Optional<RefundReceipt> findByKey(String idempotencyKey) {
        var rows = jdbc.query(
                "SELECT idempotency_key, order_id, amount FROM refund_ledger WHERE idempotency_key = ?",
                (rs, n) -> new RefundReceipt(rs.getString("idempotency_key"), rs.getString("order_id"), rs.getBigDecimal("amount")),
                idempotencyKey
        );
        return rows.stream().findFirst();
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
