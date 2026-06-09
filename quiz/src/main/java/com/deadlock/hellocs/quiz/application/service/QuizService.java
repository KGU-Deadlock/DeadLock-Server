package com.deadlock.hellocs.quiz.application.service;

import com.deadlock.hellocs.common.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.application.policy.QuizGenerationPolicy;
import com.deadlock.hellocs.quiz.application.port.in.QueryQuizInputPort;
import com.deadlock.hellocs.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.application.port.in.dto.GetQuizResult;
import com.deadlock.hellocs.quiz.application.port.in.dto.MultipleChoiceQuizResult;
import com.deadlock.hellocs.quiz.application.port.in.dto.OxQuizResult;
import com.deadlock.hellocs.quiz.application.port.in.dto.ShortAnswerQuizResult;
import com.deadlock.hellocs.quiz.application.port.in.dto.VoiceQuizResult;
import com.deadlock.hellocs.quiz.application.port.out.CommandQuizSessionOutputPort;
import com.deadlock.hellocs.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.application.port.out.QueryUserOutputPort;
import com.deadlock.hellocs.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.domain.QuizMultipleChoice;
import com.deadlock.hellocs.quiz.domain.QuizOx;
import com.deadlock.hellocs.quiz.contract.QuizLevel;
import com.deadlock.hellocs.quiz.contract.QuizMode;
import com.deadlock.hellocs.quiz.contract.QuizSession;
import com.deadlock.hellocs.quiz.contract.QuizSessionEntry;
import com.deadlock.hellocs.quiz.domain.QuizShortAnswer;
import com.deadlock.hellocs.quiz.domain.QuizVoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class QuizService implements QueryQuizInputPort {

    private final List<QuizGenerationPolicy> generationPolicies;
    private final QueryQuizOutputPort queryQuizPort;
    private final CommandQuizSessionOutputPort commandQuizSessionPort;
    private final QueryUserOutputPort queryUserPort;

    @Override
    public GetQuizResult getQuizzes(GetQuizCommand request, Long userId) {
        QuizLevel userLevel = queryUserPort.getUserLevel(userId);
        QuizGenerationPolicy policy = getGenerationPolicy(request.mode());
        List<Quiz> quizzes = policy.generate(request, queryQuizPort, userId, userLevel);
        saveQuizSession(userId, request.mode(), quizzes);
        return mapToGetQuizResult(quizzes);
    }

    private void saveQuizSession(Long userId, QuizMode mode, List<Quiz> quizzes) {
        List<Long> topicIds = quizzes.stream()
                .flatMap(q -> q.getTopicIds().stream())
                .distinct()
                .toList();

        Map<Long, QuizSessionEntry> entries = quizzes.stream()
                .collect(Collectors.toMap(
                        Quiz::getId,
                        q -> new QuizSessionEntry(
                                q.getId(),
                                q.getType(),
                                q.getContent(),
                                q.getAnswerAsString(),
                                q.getExplain()
                        )
                ));

        commandQuizSessionPort.save(new QuizSession(userId, mode, topicIds, entries));
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
