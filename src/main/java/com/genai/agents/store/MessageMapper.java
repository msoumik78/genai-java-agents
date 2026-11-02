package com.genai.agents.store;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static List<StoredMessage> toStored(List<Message> messages) {
        List<StoredMessage> stored = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof SystemMessage system) {
                stored.add(new StoredMessage("system", system.getText(), List.of()));
            } else if (message instanceof UserMessage user) {
                stored.add(new StoredMessage("user", user.getText(), List.of()));
            } else if (message instanceof ToolResponseMessage tool) {
                for (ToolResponseMessage.ToolResponse response : tool.getResponses()) {
                    stored.add(new StoredMessage("tool", response.responseData(),
                            List.of(new StoredToolCall(response.id(), response.name(), ""))));
                }
            } else if (message instanceof AssistantMessage assistant) {
                List<StoredToolCall> calls = assistant.getToolCalls().stream()
                        .map(call -> new StoredToolCall(call.id(), call.name(), call.arguments()))
                        .toList();
                stored.add(new StoredMessage("assistant", assistant.getText(), calls));
            }
        }
        return stored;
    }

    public static List<Message> toSpring(List<StoredMessage> stored) {
        List<Message> messages = new ArrayList<>();
        for (StoredMessage message : stored) {
            switch (message.role()) {
                case "system" -> messages.add(new SystemMessage(message.text()));
                case "user" -> messages.add(new UserMessage(message.text()));
                case "tool" -> {
                    StoredToolCall call = message.toolCalls().getFirst();
                    messages.add(ToolResponseMessage.builder()
                            .responses(List.of(new ToolResponseMessage.ToolResponse(call.id(), call.name(), message.text())))
                            .build());
                }
                case "assistant" -> {
                    if (message.toolCalls().isEmpty()) {
                        messages.add(new AssistantMessage(message.text() == null ? "" : message.text()));
                    } else {
                        messages.add(AssistantMessage.builder()
                                .content(message.text() == null ? "" : message.text())
                                .toolCalls(message.toolCalls().stream()
                                        .map(call -> new AssistantMessage.ToolCall(call.id(), "function", call.name(), call.arguments()))
                                        .toList())
                                .build());
                    }
                }
                default -> throw new IllegalArgumentException("Unknown message role " + message.role());
            }
        }
        return messages;
    }
}
