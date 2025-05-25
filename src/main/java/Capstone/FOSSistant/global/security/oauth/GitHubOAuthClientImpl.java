package Capstone.FOSSistant.global.security.oauth;

import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.exception.AuthException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubOAuthClientImpl implements GitHubOAuthClient {

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.github.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.github.user-info-uri}")
    private String userInfoUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String getAccessTokenFromCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(tokenUri, request, JsonNode.class);

            log.info("🔐 GitHub 토큰 응답: {}", response.getBody()); // 👈 응답 로그 출력

            String accessToken = response.getBody().path("access_token").asText();
            if (accessToken == null || accessToken.isBlank()) {
                log.error("❌ GitHub에서 access_token이 비어있습니다. 응답: {}", response.getBody());
                throw new AuthException(ErrorStatus.AUTH_GITHUB_FAIL);
            }

            return accessToken;
        } catch (Exception e) {
            log.error("❌ GitHub 액세스 토큰 요청 실패: {}", e.getMessage(), e); // 메시지 포함
            throw new AuthException(ErrorStatus.AUTH_GITHUB_FAIL);
        }
    }

    @Override
    public GitHubUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    userInfoUri,
                    HttpMethod.GET,
                    request,
                    JsonNode.class
            );

            JsonNode userJson = response.getBody();

            return new GitHubUserInfo(
                    userJson.get("id").asLong(),
                    userJson.has("email") && !userJson.get("email").isNull() ? userJson.get("email").asText() : "",
                    userJson.has("login") ? userJson.get("login").asText() : "",
                    userJson.has("avatar_url") ? userJson.get("avatar_url").asText() : ""
            );

        } catch (Exception e) {
            log.error("GitHub 사용자 정보 조회 실패", e);
            throw new AuthException(ErrorStatus.AUTH_GITHUB_FAIL);
        }
    }
}