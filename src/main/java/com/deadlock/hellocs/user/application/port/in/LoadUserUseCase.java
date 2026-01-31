package com.deadlock.hellocs.user.application.port.in;

import com.deadlock.hellocs.user.adapter.in.web.dto.ProfileResponse;
import org.springframework.modulith.NamedInterface;

@NamedInterface
public interface LoadUserUseCase {
    ProfileResponse getProfile(Long kakaoId);
    boolean isExist(Long kakaoId);
}
