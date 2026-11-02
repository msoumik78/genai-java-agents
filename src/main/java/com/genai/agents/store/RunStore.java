package com.genai.agents.store;

import java.util.Optional;

public interface RunStore {

    Optional<RunSnapshot> find(String threadId);

    void save(RunSnapshot snapshot);
}
