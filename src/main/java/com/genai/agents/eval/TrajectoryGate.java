package com.genai.agents.eval;

import java.util.List;

public final class TrajectoryGate {

    private TrajectoryGate() {
    }

    public static boolean passes(List<String> actual, List<String> expectedInOrder) {
        int index = 0;
        for (String tool : actual) {
            if (index < expectedInOrder.size() && tool.equals(expectedInOrder.get(index))) {
                index++;
            }
        }
        return index == expectedInOrder.size();
    }
}
