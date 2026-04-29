package com.deadlock.hellocs.ranking.application.service;

import com.deadlock.hellocs.ranking.application.port.in.QueryRankingUseCase;
import com.deadlock.hellocs.ranking.application.port.in.UpdateRankingUseCase;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingEntryResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingQueryType;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.UpdateRankingCommand;
import com.deadlock.hellocs.ranking.application.port.out.LoadRankingPort;
import com.deadlock.hellocs.ranking.application.port.out.LoadUserInterestPort;
import com.deadlock.hellocs.ranking.application.port.out.SaveRankingPort;
import com.deadlock.hellocs.ranking.domain.Ranking;
import com.deadlock.hellocs.ranking.domain.RankingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 랭킹 모듈의 핵심 비즈니스 로직.
 *
 * <p>인바운드 포트 두 개({@link UpdateRankingUseCase}, {@link QueryRankingUseCase})를 모두 구현하며,
 * 실제 저장소 접근은 아웃바운드 포트({@link SaveRankingPort}, {@link LoadRankingPort})에 위임함.
 * 관심 주제 기반 조회를 위해 {@link LoadUserInterestPort}도 참조함.</p>
 *
 * <h3>핵심 흐름</h3>
 * <ul>
 *     <li>update: 퀴즈 완료 이벤트 수신 시, 전체 랭킹 + 해당 퀴즈가 포함된 모든 주제 랭킹에 점수를 누적함.</li>
 *     <li>getSummary: 비로그인 사용자에게 보여줄 상위 N명 + 총 참가자 수를 반환함.</li>
 *     <li>getRankingByType: 로그인 사용자의 상위 N명 + 내 순위 + 바로 아래 2명을 함께 반환함.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RankingService implements UpdateRankingUseCase, QueryRankingUseCase {

    // 순위는 1부터 시작한다 (Redis rank의 0-base와는 별개로 서비스/응답에서는 1-based 사용).
    private static final long FIRST_RANK = 1L;

    private final SaveRankingPort saveRankingPort;
    private final LoadRankingPort loadRankingPort;
    private final LoadUserInterestPort loadUserInterestPort;

    /**
     * 점수 누적 처리.
     * 전체 랭킹(total)에 먼저 반영하고, 이어서 커맨드에 포함된 모든 topicId별 랭킹에도 동일 점수를 반영함.
     * → 한 번의 퀴즈 완료가 여러 보드의 점수에 동시에 누적되는 구조.
     */
    @Override
    public void update(UpdateRankingCommand command) {
        saveRankingPort.incrementScore(RankingKey.total(), command.userId(), command.score());
        command.topicIds().forEach(topicId ->
                saveRankingPort.incrementScore(RankingKey.topic(topicId), command.userId(), command.score())
        );
    }

    /**
     * 비로그인 요약 조회.
     * 전체 랭킹 보드에서 상위 {@code size}명과, 보드에 등록된 총 참가자 수를 함께 반환함.
     */
    @Override
    public RankingSummaryResult getSummary(int size) {
        List<RankingEntryResult> topEntries = loadRankRange(RankingKey.total(), FIRST_RANK, size);
        long total = loadRankingPort.countTotal();
        return new RankingSummaryResult(topEntries, total);
    }

    /**
     * 로그인 사용자 상세 조회.
     *
     * <ol>
     *     <li>조회 기준(ALL/INTEREST)에 따라 대상 보드 키를 결정함.</li>
     *     <li>상위 {@code size}명의 랭킹을 읽어온다.</li>
     *     <li>사용자 본인의 순위를 조회함. 점수 기록이 없으면 내 순위/주변 랭킹은 비워서 반환.</li>
     *     <li>내 순위 바로 아래(내 랭크+1 ~ 내 랭크+2) 최대 2명의 랭킹을 읽어 함께 반환함.</li>
     * </ol>
     */
    @Override
    public RankingDetailResult getRankingByType(Long userId, RankingQueryType type, int size) {
        RankingKey key = resolveKey(userId, type);
        List<RankingEntryResult> rankings = loadRankRange(key, FIRST_RANK, size);
        RankingEntryResult myRank = loadMyRank(key, userId);
        if (myRank == null) {
            return new RankingDetailResult(rankings, null, List.of());
        }
        List<RankingEntryResult> nearbyRankings = loadRankRange(key, myRank.rank() + 1, myRank.rank() + 2);
        return new RankingDetailResult(rankings, myRank, nearbyRankings);
    }

    /**
     * 조회 기준을 실제 Redis 키로 매핑함.
     * INTEREST의 경우 사용자의 첫 번째 관심 주제 ID가 반드시 존재해야 하며, 없으면 예외를 발생시킨다.
     */
    private RankingKey resolveKey(Long userId, RankingQueryType type) {
        return switch (type) {
            case ALL -> RankingKey.total();
            case INTEREST -> RankingKey.topic(
                    loadUserInterestPort.loadFirstInterestTopicId(userId).orElseThrow()
            );
        };
    }

    /** 도메인 {@link Ranking} 목록을 응답 DTO로 변환함. */
    private List<RankingEntryResult> loadRankRange(RankingKey key, long startRank, long endRank) {
        return loadRankingPort.loadByRankRange(key, startRank, endRank).stream()
                .map(r -> new RankingEntryResult(r.rank(), r.userId(), r.score()))
                .toList();
    }

    /** 사용자가 해당 보드에 아직 등록되지 않은 경우 null을 그대로 반환함. */
    private RankingEntryResult loadMyRank(RankingKey key, Long userId) {
        Ranking myRankData = loadRankingPort.loadUserRank(key, userId);
        if (myRankData == null) return null;
        return new RankingEntryResult(myRankData.rank(), myRankData.userId(), myRankData.score());
    }

}
