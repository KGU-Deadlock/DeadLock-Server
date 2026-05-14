package com.deadlock.hellocs.user.adapter.out.persistence;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryUserOutputPort;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
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
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort, QueryUserOutputPort {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User loadUserByKakaoId(Long kakaoId) {
        UserJpaEntity userJpaEntity = userRepository.findTopByKakaoIdOrderByIdDesc(kakaoId)
                .orElseThrow(() -> new CustomException(ErrorStatus._USER_NOT_FOUND));

        return userJpaEntity.toDomain();
    }

    @Override
    public User loadUserByNickname(String nickname) {
        UserJpaEntity userJpaEntity = userRepository.findByNickname(nickname)
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
        UserJpaEntity userJpaEntity = UserJpaEntity.from(user);
        userRepository.save(userJpaEntity);
    }

    @Override
    public void deleteUserByKakaoId(Long kakaoId) {
        UserJpaEntity userJpaEntity = userRepository.findTopByKakaoIdOrderByIdDesc(kakaoId)
                .orElseThrow(() -> new CustomException(ErrorStatus._USER_NOT_FOUND));
        userRepository.delete(userJpaEntity);
    }

    @Override
    public QuizLevel getUserLevel(Long kakaoId) {
        UserJpaEntity userJpaEntity = userRepository.findTopByKakaoIdOrderByIdDesc(kakaoId)
                .orElseThrow(() -> new CustomException(ErrorStatus._USER_NOT_FOUND));
        return userJpaEntity.getQuizLevel();
    }
}
