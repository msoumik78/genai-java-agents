package com.genai.agents.compaction;

import com.genai.agents.store.StoredMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadCompactorTest {

    @Test
    void compactsOneHundredTurnsAndKeepsPinnedFacts() {
        List<StoredMessage> turns = new ArrayList<>();
        turns.add(new StoredMessage("user", "Order ORD-1001. PIN: warranty-code ABC", List.of()));
        for (int i = 1; i < 100; i++) {
            String text = "x".repeat(400) + " turn " + i;
            turns.add(new StoredMessage(i % 5 == 0 ? "tool" : "user", text, List.of()));
        }

        var result = new ThreadCompactor(6, 40).compact(turns);

        assertThat(result.compacted()).isTrue();
        assertThat(result.messages()).hasSize(7);
        assertThat(result.pinnedFacts()).contains("ORD-1001", "PIN: warranty-code ABC");
        String summary = result.messages().getFirst().text();
        assertThat(summary).contains("ORD-1001").contains("warranty-code ABC");
        assertThat(result.messages().stream().filter(message -> "tool".equals(message.role())).findFirst().orElseThrow().text())
                .contains("[truncated]");
        int before = turns.stream().mapToInt(message -> message.text().length()).sum();
        int after = result.messages().stream().mapToInt(message -> message.text().length()).sum();
        assertThat(after).isLessThan(before / 5);
    }
}
