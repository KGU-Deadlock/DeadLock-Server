package com.deadlock.hellocs.quiz.quiz.application.service;

import com.deadlock.hellocs.quiz.quiz.application.policy.QuizGenerationPolicy;
import com.deadlock.hellocs.quiz.quiz.application.port.in.QueryQuizInputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService implements QueryQuizInputPort {
    
    private final List<QuizGenerationPolicy> generationPolicies;
    private final QueryQuizOutputPort queryQuizPort;
    
    @Override
    public List<Quiz> getQuizzes(GetQuizCommand request) {
        QuizGenerationPolicy policy = getGenerationPolicy(request.mode());
        return policy.generate(request, queryQuizPort);
    }
    
    //Todo: Exception 구현
    private QuizGenerationPolicy getGenerationPolicy(QuizMode mode) {
        return generationPolicies.stream()
                .filter(policy -> policy.supports(mode))
                .findFirst()
                .get();
    }
}
