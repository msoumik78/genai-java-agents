package com.genai.agents.hooks;

import com.genai.agents.config.AgentProperties;
import com.genai.agents.exception.StuckLoopException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
public class StuckToolHook implements AgentHook {

    private final int maxIdenticalToolCalls;

    @Autowired
    public StuckToolHook(AgentProperties properties) {
        this(properties.maxIdenticalToolCalls());
    }

    public StuckToolHook(int maxIdenticalToolCalls) {
        this.maxIdenticalToolCalls = maxIdenticalToolCalls;
    }

    @Override
    public void beforeTool(HarnessState state, AssistantMessage.ToolCall call) {
        int streak = state.noteToolCall(call.name() + " " + call.arguments());
        if (streak >= maxIdenticalToolCalls) {
            throw new StuckLoopException(call.name(), streak);
        }
    }
}
