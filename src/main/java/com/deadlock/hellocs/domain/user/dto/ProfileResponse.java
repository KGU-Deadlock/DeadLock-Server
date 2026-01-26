package com.deadlock.hellocs.domain.user.dto;

import java.util.List;

public record ProfileResponse(
        String profileImage,
        String nickname,
        List<String> interests
) {
}
