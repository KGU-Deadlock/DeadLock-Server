package com.deadlock.hellocs.quiz.quiz.application.service;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.quiz.application.policy.QuizGenerationPolicy;
import com.deadlock.hellocs.quiz.quiz.application.port.in.QueryQuizInputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizResult;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.MultipleChoiceQuizResult;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.OxQuizResult;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.ShortAnswerQuizResult;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.VoiceQuizResult;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.quiz.domain.QuizMultipleChoice;
import com.deadlock.hellocs.quiz.quiz.domain.QuizOx;
import com.deadlock.hellocs.quiz.quiz.domain.QuizShortAnswer;
import com.deadlock.hellocs.quiz.quiz.domain.QuizVoice;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class QuizService implements QueryQuizInputPort {

    private final List<QuizGenerationPolicy> generationPolicies;
    private final QueryQuizOutputPort queryQuizPort;

    @Override
    public GetQuizResult getQuizzes(GetQuizCommand request) {
        QuizGenerationPolicy policy = getGenerationPolicy(request.mode());
        return mapToGetQuizResult(policy.generate(request, queryQuizPort));
    }

    private QuizGenerationPolicy getGenerationPolicy(QuizMode mode) {
        return generationPolicies.stream()
                .filter(policy -> policy.supports(mode))
                .findFirst()
                .orElseThrow(() -> new CustomException(QuizErrorStatus.QUIZ_POLICY_NOT_FOUND));
    }

    private GetQuizResult mapToGetQuizResult(List<Quiz> quizzes) {
        QuizResultAccumulator accumulator = new QuizResultAccumulator();
        quizzes.forEach(accumulator::add);
        return accumulator.toResult();
    }

    private static final class QuizResultAccumulator {
        private final List<OxQuizResult> oxQuizzes = new ArrayList<>();
        private final List<MultipleChoiceQuizResult> multipleChoiceQuizzes = new ArrayList<>();
        private final List<ShortAnswerQuizResult> shortAnswerQuizzes = new ArrayList<>();
        private final List<VoiceQuizResult> voiceQuizzes = new ArrayList<>();

        private void add(Quiz quiz) {
            switch (quiz) {
                case QuizOx oxQuiz -> oxQuizzes.add(OxQuizResult.from(oxQuiz));
                case QuizMultipleChoice multipleChoiceQuiz ->
                        multipleChoiceQuizzes.add(MultipleChoiceQuizResult.from(multipleChoiceQuiz));
                case QuizShortAnswer shortAnswerQuiz ->
                        shortAnswerQuizzes.add(ShortAnswerQuizResult.from(shortAnswerQuiz));
                case QuizVoice voiceQuiz -> voiceQuizzes.add(VoiceQuizResult.from(voiceQuiz));
                default -> throw new CustomException(QuizErrorStatus.QUIZ_REQUEST_INVALID);
            }
        }

        private GetQuizResult toResult() {
            return new GetQuizResult(
                    List.copyOf(oxQuizzes),
                    List.copyOf(multipleChoiceQuizzes),
                    List.copyOf(shortAnswerQuizzes),
                    List.copyOf(voiceQuizzes)
            );
        }
    }
}
