package com.genai.agents.compaction;

import com.genai.agents.store.StoredMessage;
import com.genai.agents.store.StoredToolCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ThreadCompactor {

    private final int keepRecentMessages;
    private final int toolResultMaxChars;

    @Autowired
    public ThreadCompactor(com.genai.agents.config.AgentProperties properties) {
        this(properties.keepRecentMessages(), properties.toolResultMaxChars());
    }

    public ThreadCompactor(int keepRecentMessages, int toolResultMaxChars) {
        this.keepRecentMessages = keepRecentMessages;
        this.toolResultMaxChars = toolResultMaxChars;
    }

    public CompactionResult compact(List<StoredMessage> messages) {
        List<StoredMessage> truncated = messages.stream().map(this::truncateToolResult).toList();
        List<String> pins = pinnedFacts(truncated);
        int leading = 0;
        while (leading < truncated.size() && "system".equals(truncated.get(leading).role())) {
            leading++;
        }
        int body = truncated.size() - leading;
        if (body <= keepRecentMessages) {
            return new CompactionResult(truncated, pins, false);
        }
        List<StoredMessage> out = new ArrayList<>(truncated.subList(0, leading));
        out.add(new StoredMessage("system", "Earlier turns summarized. Pinned facts: " + String.join(" | ", pins), List.of()));
        out.addAll(truncated.subList(truncated.size() - keepRecentMessages, truncated.size()));
        return new CompactionResult(out, pins, true);
    }

    private StoredMessage truncateToolResult(StoredMessage message) {
        if (!"tool".equals(message.role()) || message.text() == null || message.text().length() <= toolResultMaxChars) {
            return message;
        }
        return new StoredMessage("tool", message.text().substring(0, toolResultMaxChars) + "...[truncated]", message.toolCalls());
    }

    private static List<String> pinnedFacts(List<StoredMessage> messages) {
        List<String> pins = new ArrayList<>();
        for (StoredMessage message : messages) {
            String text = message.text() == null ? "" : message.text();
            if (text.contains("PIN:")) {
                pins.add(text.substring(text.indexOf("PIN:")).trim());
            }
            for (StoredToolCall call : message.toolCalls()) {
                if (call.name() != null && !call.name().isBlank() && !pins.contains("tool:" + call.name())) {
                    pins.add("tool:" + call.name());
                }
            }
            if (text.contains("ORD-")) {
                int start = text.indexOf("ORD-");
                int end = start + 4;
                while (end < text.length() && Character.isDigit(text.charAt(end))) {
                    end++;
                }
                String order = text.substring(start, end);
                if (!pins.contains(order)) {
                    pins.add(order);
                }
            }
        }
        return pins;
    }

    public record CompactionResult(List<StoredMessage> messages, List<String> pinnedFacts, boolean compacted) {
    }
}
