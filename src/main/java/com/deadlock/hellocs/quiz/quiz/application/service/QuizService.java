package com.deadlock.hellocs.quiz.quiz.application.service;

import com.deadlock.hellocs.quiz.quiz.application.policy.QuizGenerationPolicy;
import com.deadlock.hellocs.quiz.quiz.application.port.in.LoadQuizInputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.LoadQuizRequest;
import com.deadlock.hellocs.quiz.quiz.application.port.out.LoadQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService implements LoadQuizInputPort {
    
    private final List<QuizGenerationPolicy> generationPolicies;
    private final LoadQuizOutputPort loadQuizPort;
    
    @Override
    public List<Quiz> loadQuizzes(LoadQuizRequest request) {
        QuizGenerationPolicy policy = findGenerationPolicy(request.mode());
        return collectQuizzesByPolicy(request, policy);
    }
    
    //Todo: Exception 구현
    private QuizGenerationPolicy findGenerationPolicy(QuizMode mode) {
        return generationPolicies.stream()
                .filter(policy -> policy.supports(mode))
                .findFirst()
                .get();
    }
    
    private List<Quiz> collectQuizzesByPolicy(LoadQuizRequest request, QuizGenerationPolicy policy) {
        List<Quiz> quizzes = new ArrayList<>();
        Map<QuizType, Integer> composition = policy.getQuizComposition();
        
        composition.forEach((type, count) -> {
            List<Quiz> foundQuizzes = loadQuizPort.findQuizzesByCriteria(
                    request.level(),
                    request.topicIds(),
                    type,
                    count
            );
            quizzes.addAll(foundQuizzes);
        });
        
        return quizzes;
    }
}

