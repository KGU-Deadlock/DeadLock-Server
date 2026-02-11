package com.deadlock.hellocs.user.adapter.in.web.dto;

import com.deadlock.hellocs.user.domain.User;

public record MyInfoResponse(
        Long id,
        String nickname,
        String profileImage
) {
    public static MyInfoResponse from(User user) {
        return new MyInfoResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImage()
        );
    }
}
