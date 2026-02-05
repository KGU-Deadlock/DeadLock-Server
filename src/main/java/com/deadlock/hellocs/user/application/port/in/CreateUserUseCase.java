package com.deadlock.hellocs.user.application.port.in;

import com.deadlock.hellocs.user.adapter.in.web.dto.UserSignUpRequest;

public interface CreateUserUseCase {
    void createUser(Long kakaoId, UserSignUpRequest userInfo);
}
