package Capstone.FOSSistant.global.service.llm;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildPrompt(String title, String body, String readme, String structure) {
        return String.format("""
               다음은 GitHub 이슈와 레포지토리 정보입니다.
                이 정보를 바탕으로 이슈를 해결하기 위한 가이드를 작성해주세요.

                -- 이슈 정보 --
                [제목]: %s
                [본문]: %s

                -- 레포지토리 README --
                %s

                -- 레포지토리 구조 (최상위 경로 기준) --
                %s

                💡 아래 형식을 따르는 JSON만 응답하세요. 다른 설명이나 텍스트는 절대 포함하지 마세요.

                예시 응답 형식:
                {
                  "title": "이슈 제목",
                  "difficulty": "easy | medium | hard | misc",
                  "description": "이슈의 핵심을 정리한 설명",
                  "solution": "이 이슈를 해결하기 위한 접근 방법",
                  "caution": "주의할 점 또는 흔한 실수"
                }

                반드시 위 JSON 형식을 따르세요. 응답에 마크다운, 설명, 문장 등을 포함하지 마세요.
               """, title, body, readme, structure);
    }
}