package com.genai.agents.hooks;

import com.genai.agents.config.AgentProperties;
import com.genai.agents.exception.TokenBudgetExceededException;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class TokenBudgetHook implements AgentHook {

    private final int maxTotalTokens;

    @Autowired
    public TokenBudgetHook(AgentProperties properties) {
        this(properties.maxTotalTokens());
    }

    public TokenBudgetHook(int maxTotalTokens) {
        this.maxTotalTokens = maxTotalTokens;
    }

    @Override
    public void afterModel(HarnessState state, org.springframework.ai.chat.model.ChatResponse response) {
        Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
        if (usage != null && usage.getTotalTokens() != null) {
            state.addTokens(usage.getTotalTokens());
        }
        if (state.totalTokens() > maxTotalTokens) {
            throw new TokenBudgetExceededException(state.totalTokens(), maxTotalTokens);
        }
    }
}
