package com.deadlock.hellocs.user.application.port.in;

import com.deadlock.hellocs.user.adapter.in.web.dto.MyInfoResponse;
import com.deadlock.hellocs.user.adapter.in.web.dto.UpdateMyInfoRequest;

public interface ManageUserUseCase {
    MyInfoResponse updateMyInfo(Long kakaoId, UpdateMyInfoRequest request);
    void deleteMyAccount(Long kakaoId);
}
