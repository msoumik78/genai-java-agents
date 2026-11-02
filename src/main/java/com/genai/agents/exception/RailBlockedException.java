package com.genai.agents.exception;

import com.genai.agents.vo.RailEvent;

public class RailBlockedException extends RuntimeException {

    private final RailEvent event;

    public RailBlockedException(RailEvent event) {
        super(event.detail());
        this.event = event;
    }

    public RailEvent event() {
        return event;
    }
}
