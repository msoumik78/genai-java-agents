package com.genai.agents.exception;

import com.genai.agents.vo.AgentFailure;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AgentExceptionHandler {

    @ExceptionHandler({LoopLimitExceededException.class, TokenBudgetExceededException.class, StuckLoopException.class})
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public AgentFailure budget(RuntimeException ex) {
        return new AgentFailure("budget", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AgentFailure badRequest(IllegalArgumentException ex) {
        return new AgentFailure("invalid", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public AgentFailure conflict(IllegalStateException ex) {
        return new AgentFailure("conflict", ex.getMessage());
    }
}
