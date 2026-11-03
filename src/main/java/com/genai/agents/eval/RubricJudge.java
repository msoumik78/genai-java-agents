package com.genai.agents.eval;

import com.genai.agents.vo.AgentRun;

import java.math.BigDecimal;
import java.util.List;

public final class RubricJudge {

    private RubricJudge() {
    }

    public static double score(AgentRun run, boolean approved, BigDecimal amount, List<String> toolsInOrder) {
        if (run.decision() == null) {
            return 0;
        }
        double score = 0;
        if (run.decision().approved() == approved) {
            score += 0.3;
        }
        if (run.decision().amount().compareTo(amount) == 0) {
            score += 0.3;
        }
        if (TrajectoryGate.passes(run.toolsCalled(), toolsInOrder)) {
            score += 0.4;
        }
        return score;
    }
}
