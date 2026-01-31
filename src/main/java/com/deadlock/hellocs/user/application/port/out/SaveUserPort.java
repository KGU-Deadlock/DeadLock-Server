package com.deadlock.hellocs.user.application.port.out;

import com.deadlock.hellocs.user.domain.User;

public interface SaveUserPort {
    void saveUser(User user);
}
