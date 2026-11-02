package com.genai.agents.hooks;

import com.genai.agents.vo.RailEvent;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;

public final class HarnessState {

    private final String orderId;
    private final List<Message> messages;
    private final List<RailEvent> rails = new ArrayList<>();
    private int modelCalls;
    private int totalTokens;
    private boolean inputChecked;
    private String lastToolSignature;
    private int identicalToolStreak;

    public HarnessState(String orderId, List<Message> messages) {
        this.orderId = orderId;
        this.messages = messages;
    }

    public String orderId() {
        return orderId;
    }

    public List<Message> messages() {
        return messages;
    }

    public List<RailEvent> rails() {
        return List.copyOf(rails);
    }

    public void record(RailEvent event) {
        rails.add(event);
    }

    public int modelCalls() {
        return modelCalls;
    }

    public void incrementModelCalls() {
        modelCalls++;
    }

    public void restore(int modelCalls, int totalTokens, List<RailEvent> rails) {
        this.modelCalls = modelCalls;
        this.totalTokens = totalTokens;
        this.rails.addAll(rails);
        this.inputChecked = true;
    }

    public int totalTokens() {
        return totalTokens;
    }

    public void addTokens(int tokens) {
        totalTokens += tokens;
    }

    public boolean inputChecked() {
        return inputChecked;
    }

    public void markInputChecked() {
        inputChecked = true;
    }

    public int noteToolCall(String signature) {
        if (signature.equals(lastToolSignature)) {
            identicalToolStreak++;
        } else {
            lastToolSignature = signature;
            identicalToolStreak = 1;
        }
        return identicalToolStreak;
    }
}
