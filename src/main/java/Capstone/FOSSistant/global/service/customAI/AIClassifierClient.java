package Capstone.FOSSistant.global.service.customAI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIClassifierClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<String> classify(String title, String body) {
        String shortBody = (body == null || body.isBlank()) ? "(no description provided)" : body;
        shortBody = shortBody.replaceAll("(?s)```.*?```", "");

        long start = System.currentTimeMillis();

        Map<String, Object> payload = Map.of(
                "model", "fossistant-v0.1.0",
                "issues", List.of(Map.of(
                        "title", title,
                        "body", shortBody
                ))
        );

        return webClient.post()
                .uri("https://api.ucyang.com/v1/fossistant/difficulty/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(result -> {
                    long end = System.currentTimeMillis();
                    log.info("[AI API 호출 + 응답 수신 시간] {}ms", (end - start));
                })
                .doOnError(e -> {
                    long end = System.currentTimeMillis();
                    log.warn("[AI API 호출 실패 시간] {}ms", (end - start));
                })
                .onErrorResume(TimeoutException.class, e -> {
                    log.warn("AI Timeout 발생", e);
                    return Mono.just("{\"results\": [{\"difficulty\": \"misc\", \"score\": 0.0}]}");
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("AI 응답 에러: {}", e.getMessage(), e);
                    return Mono.just("{\"results\": [{\"difficulty\": \"misc\", \"score\": 0.0}]}");
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("AI 기타 예외 발생", e);
                    return Mono.just("{\"results\": [{\"difficulty\": \"misc\", \"score\": 0.0}]}");
                });
    }

    // 결과 파싱 헬퍼 (선택적)
    public String extractDifficultyFromResult(String resultJson) {
        try {
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode results = root.get("results");
            if (results != null && results.isArray() && results.size() > 0) {
                return results.get(0).get("difficulty").asText().toLowerCase();
            }
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패 - result: {}", resultJson, e);
        }
        return "misc";
    }
}