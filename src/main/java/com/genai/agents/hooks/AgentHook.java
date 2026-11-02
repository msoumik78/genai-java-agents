package com.genai.agents.hooks;

import com.genai.agents.vo.RefundDecision;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;

public interface AgentHook {

    default void beforeModel(HarnessState state) {
    }

    default void afterModel(HarnessState state, ChatResponse response) {
    }

    default void beforeTool(HarnessState state, AssistantMessage.ToolCall call) {
    }

    default void afterTool(HarnessState state, AssistantMessage.ToolCall call, String result) {
    }

    default void afterDecision(HarnessState state, RefundDecision decision) {
    }
}
