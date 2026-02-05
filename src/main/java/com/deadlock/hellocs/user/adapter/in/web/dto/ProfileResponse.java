package com.deadlock.hellocs.user.adapter.in.web.dto;

import java.util.List;

public record ProfileResponse(
        String profileImage,
        String nickname,
        List<String> interests
) {
}
