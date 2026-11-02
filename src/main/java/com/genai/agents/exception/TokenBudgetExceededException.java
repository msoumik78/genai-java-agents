package com.genai.agents.exception;

public class TokenBudgetExceededException extends RuntimeException {

    public TokenBudgetExceededException(int used, int maxTotalTokens) {
        super("Token budget exhausted: " + used + " tokens used, limit " + maxTotalTokens);
    }
}
