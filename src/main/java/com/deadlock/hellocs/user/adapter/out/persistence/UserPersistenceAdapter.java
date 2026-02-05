package com.deadlock.hellocs.user.adapter.out.persistence;

import com.deadlock.hellocs.user.adapter.out.persistence.entity.UserJpaJpaEntity;
import com.deadlock.hellocs.user.application.port.out.LoadUserPort;
import com.deadlock.hellocs.user.application.port.out.SaveUserPort;
import com.deadlock.hellocs.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserRepository userRepository;

    @Override
    public User loadUserByKakaoId(Long kakaoId) {
        UserJpaJpaEntity userJpaEntity = userRepository.findByKakaoId(kakaoId)
                .orElseThrow();

        return userJpaEntity.toDomain();
    }

    @Override
    public User loadUserByNickname(String nickname) {
        UserJpaJpaEntity userJpaEntity = userRepository.findByNickname(nickname)
                .orElseThrow();
        return userJpaEntity.toDomain();
    }

    @Override
    public void saveUser(User user) {
        UserJpaJpaEntity userJpaEntity = UserJpaJpaEntity.from(user);
        userRepository.save(userJpaEntity);
    }
}
