package com.deadlock.hellocs.ranking.application.port.in;

import com.deadlock.hellocs.ranking.application.port.in.dto.UpdateRankingCommand;

public interface UpdateRankingUseCase {

    void update(UpdateRankingCommand command);
}
