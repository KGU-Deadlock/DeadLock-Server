package com.deadlock.hellocs.user.application.port.in;

import com.deadlock.hellocs.user.application.port.in.dto.UserSignUpCommand;

public interface CreateUserUseCase {
    void createUser(Long kakaoId, UserSignUpCommand command);
}
