package com.deadlock.hellocs.ranking.application.port.out;

import java.util.Optional;

public interface LoadUserInterestPort {

    Optional<Long> loadFirstInterestTopicId(Long userId);
}
