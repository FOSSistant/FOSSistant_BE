package Capstone.FOSSistant.global.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIClassifierClient {

    private final WebClient webClient;

    public Mono<String> classify(String title, String body) {
        String shortBody = (body == null || body.isBlank()) ? "(no description provided)" : body;
        shortBody = shortBody.replaceAll("(?s)```.*?```", "");

        return webClient.post()
                .uri("https://api.ucyang.com/v1/fossistant/difficulty/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("title", title, "body", shortBody))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(e -> {
                    log.error("AI 호출 실패 → fallback to MISC", e);
                    return Mono.just("{\"difficulty\": \"misc\"}");
                });
    }
}