package Capstone.FOSSistant.global.service.llm;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    public String buildPrompt(String title, String body, String readme, String structure, List<String> relatedLinks) {
        return String.format("""
            당신은 오픈소스 프로젝트의 이슈를 분석하고, 아래 6가지 항목에 대해 JSON 형태로 응답하는 AI 비서입니다. 응답은 반드시 한국어로 작성된 JSON 형식만 포함해야 합니다. 설명 텍스트를 추가하지 마세요.

            -- 이슈 정보 --
            [제목]: %s
            [본문 (중요 문장은 꼭 1개 이상 추출해서 응답)]:
            %s

            -- 레포지토리 README --
            %s

            -- 레포지토리 구조 (최상위 경로 기준) --
            %s

            -- 기여 가이드 및 템플릿 링크 (있다면) --
            %s

            아래 JSON 형식에 맞춰 응답하세요. 절대 다른 텍스트는 포함하지 마세요. 마크다운문법으로 한국어로 작성하세요.

            {
              "title": "이슈 제목",
              "difficulty": "easy | medium | hard | misc",
              "highlightedBody": "중요한 문장 1개 이상 추출하기",
              "description": "이슈 설명",
              "solution": "이슈 해결 방법(이스케이프 문자로 문단 나누기 필요, 마크다운으로 강조)",
              "relatedLinks": "- [CONTRIBUTING.md](...)\n- [Issue Template](...)"
            }
            """,
                title,
                body,
                readme,
                structure,
                String.join("\n- ", relatedLinks)
        );
    }
}