package com.genai.agents.store;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRunStore implements RunStore {

    private final ConcurrentHashMap<String, RunSnapshot> runs = new ConcurrentHashMap<>();

    @Override
    public Optional<RunSnapshot> find(String threadId) {
        return Optional.ofNullable(runs.get(threadId));
    }

    @Override
    public void save(RunSnapshot snapshot) {
        runs.put(snapshot.threadId(), snapshot);
    }
}
