package com.deadlock.hellocs.global.client.dto;

import java.util.List;

public record UserDetail(
        Long kakaoId,
        List<Long> interestTopicIds
) {}
