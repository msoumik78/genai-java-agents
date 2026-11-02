package com.genai.agents.store;

import java.util.List;

public record StoredMessage(String role, String text, List<StoredToolCall> toolCalls) {

    public StoredMessage {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }
}
