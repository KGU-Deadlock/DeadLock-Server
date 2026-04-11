package com.deadlock.hellocs.ranking.application.port.out;

import java.util.List;

public interface UpdateRankingPort {
    // 수정 표시
    boolean increaseScoreIfAbsent(String gradingLogId, Long kakaoId, int score, List<Long> topicIds);
}
