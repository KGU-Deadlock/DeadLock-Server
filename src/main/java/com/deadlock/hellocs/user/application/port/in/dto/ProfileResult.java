package com.deadlock.hellocs.user.application.port.in.dto;

import java.util.List;

public record ProfileResult(
        String profileImage,
        String nickname,
        List<String> interests
) {
}
