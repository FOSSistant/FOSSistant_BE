package Capstone.FOSSistant.global.service.llm;

import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiChatClient {

    @Value("${spring.gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public IssueGuideResponseDTO call(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        String responseBody = response.getBody();

        log.debug("Gemini 응답 원문:\n{}", responseBody);

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            for (JsonNode candidate : candidates) {
                String rawText = candidate
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText("");

                if (rawText.isBlank()) continue;

                // ```json 또는 ``` 제거
                String cleaned = rawText
                        .replaceAll("(?s)```json", "")
                        .replaceAll("(?s)```", "")
                        .trim();

                try {
                    IssueGuideResponseDTO dto = objectMapper.readValue(cleaned, IssueGuideResponseDTO.class);
                    log.debug("Gemini 응답 파싱 성공");
                    return dto;
                } catch (Exception parseEx) {
                    log.warn("Gemini 후보 응답 파싱 실패: {}", cleaned);
                }
            }

            throw new RuntimeException("Gemini 응답 내 파싱 가능한 JSON 후보가 없습니다.");

        } catch (Exception e) {
            log.error("Gemini 응답 파싱 전체 실패", e);
            log.error("원문 응답: {}", responseBody);
            throw new RuntimeException("Gemini JSON 응답 파싱 실패: " + e.getMessage());
        }
    }
}