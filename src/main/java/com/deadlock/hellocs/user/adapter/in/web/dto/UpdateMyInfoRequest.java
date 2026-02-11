package com.deadlock.hellocs.user.adapter.in.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateMyInfoRequest(
        @Size(max = 12, message = "닉네임은 최대 12자까지 가능합니다.")
        String nickname,

        @Size(max = 500, message = "프로필 이미지 URL은 최대 500자까지 가능합니다.")
        String profileImage
) {
}
