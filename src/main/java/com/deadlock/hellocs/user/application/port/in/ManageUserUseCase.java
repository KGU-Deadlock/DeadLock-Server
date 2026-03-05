package com.deadlock.hellocs.user.application.port.in;

import com.deadlock.hellocs.user.application.port.in.dto.UpdateMyInfoCommand;

public interface ManageUserUseCase {
    void updateMyInfo(Long kakaoId, UpdateMyInfoCommand command);
    void deleteMyAccount(Long kakaoId);
}
