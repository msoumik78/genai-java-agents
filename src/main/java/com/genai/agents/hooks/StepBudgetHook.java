package com.genai.agents.hooks;

import com.genai.agents.exception.LoopLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class StepBudgetHook implements AgentHook {

    private final int maxIterations;

    @Autowired
    public StepBudgetHook(com.genai.agents.config.AgentProperties properties) {
        this(properties.maxIterations());
    }

    public StepBudgetHook(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    @Override
    public void beforeModel(HarnessState state) {
        if (state.modelCalls() >= maxIterations) {
            throw new LoopLimitExceededException(maxIterations);
        }
    }
}
