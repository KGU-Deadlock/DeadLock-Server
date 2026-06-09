package com.deadlock.hellocs.quiz.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.regex.Pattern;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizMultipleChoice extends Quiz {
    private static final String CHOICE_DELIMITER = "<;;;>";

    private String content;
    private Integer answer;
    private String explain;
    private List<String> choice;
    
    @Override
    public String getAnswerAsString() {
        return String.valueOf(answer);
    }

    public static List<String> splitChoices(String rawChoice) {
        if (rawChoice == null || rawChoice.isBlank()) {
            return List.of();
        }

        return Pattern.compile(Pattern.quote(CHOICE_DELIMITER))
                .splitAsStream(rawChoice)
                .map(String::trim)
                .filter(option -> !option.isBlank())
                .toList();
    }
    
    @Override
    public String getContent() {
        return content;
    }
    
    @Override
    public String getExplain() {
        return explain;
    }
}
