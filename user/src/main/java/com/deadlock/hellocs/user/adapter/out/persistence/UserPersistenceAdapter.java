package com.deadlock.hellocs.user.adapter.out.persistence;

import com.deadlock.hellocs.common.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.common.exception.CustomException;
import com.deadlock.hellocs.user.adapter.out.persistence.entity.UserJpaEntity;
import com.deadlock.hellocs.user.application.port.out.LoadUserPort;
import com.deadlock.hellocs.user.application.port.out.SaveUserPort;
import com.deadlock.hellocs.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User loadUserByKakaoId(Long kakaoId) {
        UserJpaEntity userJpaEntity = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new CustomException(ErrorStatus._USER_NOT_FOUND));
        return userJpaEntity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> loadUsersByKakaoIds(List<Long> kakaoIds) {
        return userRepository.findByKakaoIdIn(kakaoIds).stream()
                .map(UserJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    @Override
    public boolean existsByKakaoId(Long kakaoId) {
        return userRepository.existsByKakaoId(kakaoId);
    }

    @Override
    public void saveUser(User user) {
        userRepository.save(UserJpaEntity.from(user));
    }

    @Override
    public void deleteUserByKakaoId(Long kakaoId) {
        UserJpaEntity userJpaEntity = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new CustomException(ErrorStatus._USER_NOT_FOUND));
        userRepository.delete(userJpaEntity);
    }
}
