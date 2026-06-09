package com.deadlock.hellocs.ranking.application.port.out;

import java.util.Optional;

/**
 * 사용자의 관심 주제를 조회하기 위한 아웃바운드 포트.
 * 구현체: {@link com.deadlock.hellocs.ranking.adapter.out.user.UserInterestRestAdapter}
 */
public interface LoadUserInterestPort {

    /** 해당 사용자의 첫 번째 관심 주제 ID. 관심 주제가 없으면 {@link Optional#empty()}. */
    Optional<Long> loadFirstInterestTopicId(Long userId);
}
