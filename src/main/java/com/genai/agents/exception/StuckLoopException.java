package com.genai.agents.exception;

public class StuckLoopException extends RuntimeException {

    public StuckLoopException(String toolName, int repeats) {
        super("Stuck loop: " + toolName + " repeated " + repeats + " times with the same arguments");
    }
}
