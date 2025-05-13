package Capstone.FOSSistant.global.service.llm;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildPrompt(String title, String body, String readme, String structure) {
        return String.format("""
    아래는 GitHub 이슈 및 레포지토리 정보입니다.

    🔹 이슈 제목: %s
    🔹 이슈 설명: %s

    🔸 레포지토리 README:
    %s

    🔸 레포지토리 구조:
    %s

    🔽 반드시 아래와 같은 순수 JSON 형식으로만 응답하세요.
    설명 없이 JSON만 반환하세요. 마크다운, 텍스트 없음.

    예시 형식:
    {
      "title": "이슈 제목",
      "difficulty": "상",
      "description": "이슈에 대한 설명",
      "solution": "이슈 해결 방법",
      "caution": "주의사항"
    }
    """, title, body, readme, structure);
    }
}