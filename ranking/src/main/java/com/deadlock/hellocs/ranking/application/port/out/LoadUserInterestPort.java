package com.deadlock.hellocs.ranking.application.port.out;

import java.util.Optional;

/**
 * 사용자의 관심 주제를 조회하기 위한 아웃바운드 포트.
 *
 * <p>랭킹 모듈은 user 모듈에 직접 의존하지 않고 이 포트를 통해서만
 * 관심 주제 정보를 읽어온다. 실제 구현은 외부 모듈(예: global/ranking/UserInterestAdapter)에서 제공된다.</p>
 */
public interface LoadUserInterestPort {

    /**
     * 해당 사용자의 "첫 번째" 관심 주제 ID를 반환함.
     * 관심 주제가 없으면 {@link Optional#empty()}.
     */
    Optional<Long> loadFirstInterestTopicId(Long userId);
}
