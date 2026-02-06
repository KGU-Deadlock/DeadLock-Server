package com.deadlock.hellocs.quiz.application;

import com.deadlock.hellocs.quiz.QuizType;
import com.deadlock.hellocs.quiz.application.policy.quiz.QuizGenerationPolicy;
import com.deadlock.hellocs.quiz.application.port.in.LoadQuizUseCase;
import com.deadlock.hellocs.quiz.application.port.in.QuizMode;
import com.deadlock.hellocs.quiz.application.port.in.request.LoadQuizRequest;
import com.deadlock.hellocs.quiz.application.port.out.LoadQuizPort;
import com.deadlock.hellocs.quiz.domain.Quiz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService implements LoadQuizUseCase {

    private final List<QuizGenerationPolicy> policies;
    private final LoadQuizPort loadQuizPort;

    @Override
    public List<Quiz> loadQuizzes(LoadQuizRequest request) {
        QuizGenerationPolicy policy = findPolicy(request.mode());
        return collectQuizzesByPolicy(request, policy);
    }

    private QuizGenerationPolicy findPolicy(QuizMode mode) {
        return policies.stream()
                .filter(p -> p.supports(mode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported quiz mode: " + mode));
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
