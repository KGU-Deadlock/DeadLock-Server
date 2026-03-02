package com.deadlock.hellocs.ranking.application.port.in;

import com.deadlock.hellocs.ranking.application.port.in.dto.ApplyRankingScoreCommand;
import jakarta.validation.Valid;

public interface ApplyRankingScoreInputPort {
    void apply(@Valid ApplyRankingScoreCommand command);
}
