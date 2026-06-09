package com.deadlock.hellocs.topic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class Topic {
    private final Long id;
    private final String name;
}
