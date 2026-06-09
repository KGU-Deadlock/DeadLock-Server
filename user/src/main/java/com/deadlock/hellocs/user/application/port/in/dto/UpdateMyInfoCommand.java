package com.deadlock.hellocs.user.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateMyInfoCommand(
        @Size(min = 1, max = 12, message = "닉네임은 1자 이상 12자 이하로 입력해주세요.")
        @Pattern(regexp = ".*\\S.*", message = "닉네임은 빈 값으로 입력할 수 없습니다.")
        @JsonAlias({"name", "userName", "username"}) String nickname,

        @Size(min = 1, max = 500, message = "프로필 이미지 URL은 1자 이상 500자 이하로 입력해주세요.")
        @Pattern(regexp = ".*\\S.*", message = "프로필 이미지는 빈 값으로 입력할 수 없습니다.")
        String profileImage,

        @Size(min = 1, message = "interestTopicIds는 비어 있을 수 없습니다.")
        List<@NotNull(message = "interestTopicIds 항목은 null일 수 없습니다.") Long> interestTopicIds
) {
}
