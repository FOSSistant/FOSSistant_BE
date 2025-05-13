package Capstone.FOSSistant.global.service.llm;

import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

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

        System.out.println("Gemini 응답 원문:");
        System.out.println(responseBody);

        try {
            // JSON에서 text 필드만 꺼내기
            String rawText = objectMapper.readTree(responseBody)
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // 2. ```json ~ ``` 제거
            String cleaned = rawText
                    .replaceAll("(?s)```json", "")
                    .replaceAll("(?s)```", "")
                    .trim();

            System.out.println("정제된 JSON:");
            System.out.println(cleaned);

            // 3. DTO로 파싱
            return objectMapper.readValue(cleaned, IssueGuideResponseDTO.class);

        } catch (Exception e) {
            System.err.println("Gemini 응답 파싱 실패:");
            e.printStackTrace();
            System.err.println("응답 본문:");
            System.err.println(responseBody);
            throw new RuntimeException("Gemini JSON 응답 파싱 실패: " + e.getMessage());
        }
    }
}