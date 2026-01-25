package com.deadlock.hellocs.domain.user.dto;

import com.deadlock.hellocs.domain.interest.entity.Interest;
import java.util.List;

public record MyProfileResponse(
        String profileImage,
        String nickname,
        List<Interest> interests
) {
}
