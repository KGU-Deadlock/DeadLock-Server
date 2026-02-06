package com.deadlock.hellocs.quiz.application;

import com.deadlock.hellocs.quiz.QuizType;
import com.deadlock.hellocs.quiz.application.policy.grade.GradingPolicy;
import com.deadlock.hellocs.quiz.application.port.in.SubmitQuizUseCase;
import com.deadlock.hellocs.quiz.application.port.in.request.UserAnswer;
import com.deadlock.hellocs.quiz.application.port.in.response.GradingResult;
import com.deadlock.hellocs.quiz.application.port.out.LoadQuizPort;
import com.deadlock.hellocs.quiz.domain.Quiz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradingService implements SubmitQuizUseCase {

    private final LoadQuizPort loadQuizPort;
    private final List<GradingPolicy> gradingPolicies;

    @Override
    public List<GradingResult> submitAnswers(List<UserAnswer> answers) {
        Map<Long, Quiz> quizMap = loadQuizzesMap(answers);
        List<GradingResult> results = new ArrayList<>();

        for (UserAnswer answer : answers) {
            Quiz quiz = quizMap.get(answer.quizId());
            results.add(grade(answer, quiz));
        }

        return results;
    }

    private Map<Long, Quiz> loadQuizzesMap(List<UserAnswer> answers) {
        List<Long> quizIds = answers.stream()
                .map(UserAnswer::quizId)
                .toList();

        return loadQuizPort.findAllByIds(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getId, Function.identity()));
    }

    private GradingResult grade(UserAnswer userAnswer, Quiz quiz) {
        GradingPolicy policy = findGradingPolicy(quiz.getType());
        return policy.grade(quiz, userAnswer.answer());
    }

    private GradingPolicy findGradingPolicy(QuizType quizType) {
        return gradingPolicies.stream()
                .filter(policy -> policy.supports(quizType))
                .findFirst()
                .get();
    }
}
