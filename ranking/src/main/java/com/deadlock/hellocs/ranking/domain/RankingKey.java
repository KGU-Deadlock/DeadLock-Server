package com.deadlock.hellocs.ranking.domain;

/**
 * 랭킹 보드를 식별하는 sealed 인터페이스.
 *
 * <p>랭킹은 "전체 랭킹"과 "주제(Topic)별 랭킹" 두 종류로 나뉘며,
 * 각각이 서로 다른 Redis ZSet 키 공간에 저장된다.
 * 서비스 계층에서는 도메인 타입만 다루고, 실제 Redis 키 문자열 조합은
 * 이 인터페이스의 구현체가 책임진다.</p>
 *
 * <ul>
 *     <li>{@link Total}  → {@code ranking:total}</li>
 *     <li>{@link Topic}  → {@code ranking:topic:{topicId}}</li>
 * </ul>
 */
public sealed interface RankingKey permits RankingKey.Total, RankingKey.Topic {

    /** Redis ZSet 키로 사용할 문자열을 반환함. */
    String redisKey();

    /** 전체 사용자 대상 랭킹 키를 생성함. */
    static RankingKey total() {
        return new Total();
    }

    /** 특정 주제(topic)별 랭킹 키를 생성함. */
    static RankingKey topic(Long topicId) {
        return new Topic(topicId);
    }

    /** 전체 랭킹을 표현하는 단일 키. */
    record Total() implements RankingKey {
        @Override
        public String redisKey() {
            return "ranking:total";
        }
    }

    /** 주제 단위로 분리된 랭킹 키. topicId가 다르면 서로 다른 ZSet이 된다. */
    record Topic(Long topicId) implements RankingKey {
        @Override
        public String redisKey() {
            return "ranking:topic:" + topicId;
        }
    }
}
