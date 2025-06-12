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
        String shortBody = (body == null || body.isBlank()) ? "" : body;
        shortBody = shortBody.replaceAll("(?s)```.*?```", "");

        long start = System.currentTimeMillis();

        Map<String, Object> payload = Map.of(
                "model", "fossistant-v0.2.0",
                "issues", List.of(Map.of(
                        "title", title,
                        "body", shortBody
                ))
        );


        try {
            String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            log.info("전송 payload\n {}", jsonPayload);
        }   catch(Exception e) {
                log.warn("로깅 오류", e);
        }


        return webClient.post()
                .uri("https://api.ucyang.com/v1/fossistant/difficulty/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60))
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

    // Batch 분류 요청
    public Mono<String> classifyBatch(List<String[]> titleBodyList) {
        List<Map<String, String>> issues = titleBodyList.stream()
                .map(pair -> Map.of(
                        "title", pair[0],
                        "body", (pair[1] == null || pair[1].isBlank())
                                ? ""
                                : pair[1].replaceAll("(?s)```.*?```", "")
                ))
                .toList();

        Map<String, Object> payload = Map.of(
                "model", "fossistant-v0.2.0",
                "issues", issues
        );

        try {
            String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            log.info("전송 batch payload\n {}", jsonPayload);
        } catch (Exception e) {
            log.warn("batch 로깅 오류", e);
        }

        long start = System.currentTimeMillis();

        return webClient.post()
                .uri("https://api.ucyang.com/v1/fossistant/difficulty/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(50))
                .doOnSuccess( result -> {
                    long end = System.currentTimeMillis();
                    log.info("[AI Batch API 호출 + 응답 수신 시간] {}ms", (end - start));
                })
                .doOnError(e -> {
                    long end = System.currentTimeMillis();
                    log.warn("[AI Batch API 호출 실패 시간] {}ms", (end - start));
                })
                .onErrorResume(TimeoutException.class, e -> {
                    log.warn("AI Batch Timeout 발생", e);
                    return Mono.just("{\"results\": []}");
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("AI Batch 응답 에러: {}", e.getMessage(), e);
                    return Mono.just("{\"results\": []}");
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("AI Batch 기타 예외 발생", e);
                    return Mono.just("{\"results\": []}");
                });
    }
    /**
     * 단건 기본값: difficulty="misc" 하나를 리턴
     */
    public Mono<String> defaultSingleResult() {
        return Mono.just("{\"results\":[{\"difficulty\":\"misc\",\"score\":0.0}]}");
    }

    /**
     * 배치 기본값: 빈 리스트
     */
    public Mono<String> defaultBatchResult() {
        return Mono.just("{\"results\":[]}");
    }
}