package com.deadlock.hellocs.ranking.application.port.in;

import com.deadlock.hellocs.ranking.application.port.in.dto.ApplyRankingScoreCommand;

public interface ApplyRankingScoreInputPort {
    void apply(ApplyRankingScoreCommand command);
}
