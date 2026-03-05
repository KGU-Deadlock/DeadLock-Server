package com.deadlock.hellocs.user.adapter.out.persistence;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.user.adapter.out.persistence.entity.UserJpaJpaEntity;
import com.deadlock.hellocs.user.application.port.out.LoadUserPort;
import com.deadlock.hellocs.user.application.port.out.SaveUserPort;
import com.deadlock.hellocs.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserRepository userRepository;

    @Override
    public User loadUserByKakaoId(Long kakaoId) {
        UserJpaJpaEntity userJpaEntity = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new CustomException(ErrorStatus._USER_NOT_FOUND));

        return userJpaEntity.toDomain();
    }

    @Override
    public User loadUserByNickname(String nickname) {
        UserJpaJpaEntity userJpaEntity = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new CustomException(ErrorStatus._USER_NOT_FOUND));
        return userJpaEntity.toDomain();
    }

    @Override
    public List<User> loadUsersByKakaoIds(List<Long> kakaoIds) {
        return userRepository.findByKakaoIdIn(kakaoIds).stream()
                .map(UserJpaJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    @Override
    public void saveUser(User user) {
        UserJpaJpaEntity userJpaEntity = UserJpaJpaEntity.from(user);
        userRepository.save(userJpaEntity);
    }

    @Override
    public void deleteUserByKakaoId(Long kakaoId) {
        UserJpaJpaEntity userJpaEntity = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new CustomException(ErrorStatus._USER_NOT_FOUND));
        userRepository.delete(userJpaEntity);
    }
}
