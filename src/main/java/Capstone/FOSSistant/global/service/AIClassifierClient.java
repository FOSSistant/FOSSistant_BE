package Capstone.FOSSistant.global.service;


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

    public Mono<String> classify(String title, String body) {
        String shortBody = (body == null || body.isBlank()) ? "(no description provided)" : body;
        shortBody = shortBody.replaceAll("(?s)```.*?```", "");

        long start = System.currentTimeMillis();

        Map<String, Object> payload = Map.of(
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
                    return Mono.just("{\"difficulty\": \"misc\"}");
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("AI 응답 에러: {}", e.getMessage(), e);
                    return Mono.just("{\"difficulty\": \"misc\"}");
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("AI 기타 예외 발생", e);
                    return Mono.just("{\"difficulty\": \"misc\"}");
                });
    }
}