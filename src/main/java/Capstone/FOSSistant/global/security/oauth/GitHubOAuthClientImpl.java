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
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

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
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(
                Map.of(
                        "client_id", clientId,
                        "client_secret", clientSecret,
                        "code", code
                ), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(tokenUri, request, JsonNode.class);
            String accessToken = response.getBody().path("access_token").asText();
            if (accessToken == null || accessToken.isBlank()) {
                throw new AuthException(ErrorStatus.AUTH_GITHUB_FAIL);
            }
            return accessToken;
        } catch (Exception e) {
            log.error("GitHub 액세스 토큰 요청 실패", e);
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