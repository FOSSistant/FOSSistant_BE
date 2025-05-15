package Capstone.FOSSistant.global.service;

import Capstone.FOSSistant.global.web.dto.IssueList.ClassificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIClassifierClient {

    private final RestTemplate restTemplate;

    public String classify(String title, String body) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // 본문 정리 (코드 블록 제거, null 방지)
//            String shortBody = (body == null ? "" : body.replaceAll("(?s)```.*?```", "").trim());
            String shortBody = (body == null || body.isBlank()) ? "(no description provided)" : body;
            if (shortBody.isBlank()) {
                shortBody = "(no description provided)";
            }

            // JSON 직렬화
            String json = mapper.writeValueAsString(Map.of(
                    "title", title,
                    "body", shortBody
            ));

            log.debug("AI 요청 JSON: {}", json);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.ucyang.com/v1/fossistant/difficulty/",
                    entity,
                    String.class
            );
            log.debug("AI 응답 JSON 바디: {}", response.getBody());
            String responseJson = response.getBody();
            return mapper.readTree(responseJson).get("difficulty").asText();
        } catch (Exception e) {
            log.error("AI 분류 API 호출 실패", e);
            throw new RuntimeException("AI 분류 API 호출 실패", e);
        }
    }

}