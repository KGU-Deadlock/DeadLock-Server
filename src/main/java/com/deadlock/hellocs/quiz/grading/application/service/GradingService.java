package com.deadlock.hellocs.quiz.grading.application.service;

import com.deadlock.hellocs.quiz.grading.application.policy.GradingPolicy;
import com.deadlock.hellocs.quiz.grading.application.port.in.SubmitAnswerInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserAnswer;
import com.deadlock.hellocs.quiz.grading.domain.GradingResult;
import com.deadlock.hellocs.quiz.quiz.application.port.in.LoadQuizInputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.out.LoadQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 채점 서비스
 * 
 * 책임:
 * - 답안 제출 및 채점 비즈니스 로직
 * - 채점 정책에 따른 채점 수행
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradingService implements SubmitAnswerInputPort {
    
    private final LoadQuizOutputPort loadQuizPort;
    private final List<GradingPolicy> gradingPolicies;
    
    @Override
    public List<GradingResult> submitAnswers(List<UserAnswer> answers) {
        Map<Long, Quiz> quizMap = loadQuizzesMap(answers);
        
        return answers.stream()
                .map(answer -> gradeAnswer(answer, quizMap.get(answer.quizId())))
                .toList();
    }
    
    private Map<Long, Quiz> loadQuizzesMap(List<UserAnswer> answers) {
        List<Long> quizIds = answers.stream()
                .map(UserAnswer::quizId)
                .toList();
        
        return loadQuizPort.findAllByIds(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getId, Function.identity()));
    }
    
    private GradingResult gradeAnswer(UserAnswer userAnswer, Quiz quiz) {
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

