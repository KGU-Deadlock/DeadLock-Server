package com.deadlock.hellocs.user.adapter.out.auth;

import com.deadlock.hellocs.global.auth.port.UserAuthPort;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAuthAdapter implements UserAuthPort {

    private final LoadUserUseCase loadUserUseCase;

    @Override
    public boolean isExist(Long kakaoId) {
        return loadUserUseCase.isExist(kakaoId);
    }

    @Override
    public String getUserRoleName(Long kakaoId) {
        return loadUserUseCase.getUserRole(kakaoId).name();
    }
}
