package com.deadlock.hellocs.ranking.domain;

public sealed interface RankingKey permits RankingKey.Total, RankingKey.Topic {

    String redisKey();

    static RankingKey total() {
        return new Total();
    }

    static RankingKey topic(Long topicId) {
        return new Topic(topicId);
    }

    record Total() implements RankingKey {
        @Override
        public String redisKey() {
            return "ranking:total";
        }
    }

    record Topic(Long topicId) implements RankingKey {
        @Override
        public String redisKey() {
            return "ranking:topic:" + topicId;
        }
    }
}
