package com.genai.agents.exception;

public class LoopLimitExceededException extends RuntimeException {

    private final int maxIterations;

    public LoopLimitExceededException(int maxIterations) {
        super("Agent stopped after " + maxIterations + " model calls");
        this.maxIterations = maxIterations;
    }

    public int maxIterations() {
        return maxIterations;
    }
}
