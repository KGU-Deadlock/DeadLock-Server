package com.deadlock.hellocs.quiz.quiz.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizVoice extends Quiz {
    private String content;        // 음성 파일 URL 또는 경로
    private String answer;  // 정답 텍스트
    private String explain;
    private String contentText;    // 음성을 텍스트로 변환한 내용 (선택적)
    
    @Override
    public String getAnswerAsString() {
        return answer;
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
