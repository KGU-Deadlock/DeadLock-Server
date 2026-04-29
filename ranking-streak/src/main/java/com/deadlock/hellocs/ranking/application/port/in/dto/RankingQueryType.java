package com.deadlock.hellocs.ranking.application.port.in.dto;

/**
 * 랭킹 상세 조회 시 어떤 보드를 조회할지 지정하는 필터 타입.
 *
 * <ul>
 *     <li>{@link #ALL}      — 전체 사용자 대상 랭킹 (ranking:total)</li>
 *     <li>{@link #INTEREST} — 로그인 사용자의 첫 번째 관심 주제를 기준으로 한 랭킹 (ranking:topic:{id})</li>
 * </ul>
 */
public enum RankingQueryType {
    ALL,
    INTEREST
}
