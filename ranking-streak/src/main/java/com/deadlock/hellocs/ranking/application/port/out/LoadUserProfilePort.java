package com.deadlock.hellocs.ranking.application.port.out;

import java.util.List;

public interface LoadUserProfilePort {
    UserProfile loadProfile(Long userId);
    List<UserProfile> loadProfiles(List<Long> userIds);

    record UserProfile(
            Long userId,
            String nickname,
            String profileImage,
            List<String> interests
    ) {}
}
