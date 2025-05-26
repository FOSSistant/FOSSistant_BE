package Capstone.FOSSistant.global.security.oauth;

import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.exception.AuthException;
import com.fasterxml.jackson.databind.JsonNode;
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

    @Override
    public String getAccessTokenFromCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id",     clientId);
        form.add("client_secret", clientSecret);
        form.add("code",          code);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<JsonNode> resp = restTemplate.postForEntity(tokenUri, req, JsonNode.class);
            JsonNode body = resp.getBody();
            log.info("GitHub 토큰 응답: {}", body);

            String accessToken = body.path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                log.error("GitHub에서 access_token이 비어있습니다. 전체 응답: {}", body);
                throw new AuthException(ErrorStatus.AUTH_GITHUB_FAIL);
            }
            return accessToken;
        } catch (AuthException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("GitHub 액세스 토큰 요청 중 에러", e);
            throw new AuthException(ErrorStatus.AUTH_GITHUB_FAIL);
        }
    }

    @Override
    public GitHubUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> req = new HttpEntity<>(headers);

        try {
            // 1) 기본 프로필 조회
            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    userInfoUri,
                    HttpMethod.GET,
                    req,
                    JsonNode.class
            );
            JsonNode userJson = resp.getBody();
            log.info("GitHub 사용자 정보 응답: {}", userJson);

            // 2) 이메일 추출
            String email = userJson.path("email").asText("");
            if (email.isBlank()) {
                // 숨김 이메일 보완 조회
                ResponseEntity<JsonNode> emailsResp = restTemplate.exchange(
                        userInfoUri + "/emails",
                        HttpMethod.GET,
                        req,
                        JsonNode.class
                );
                for (JsonNode emailNode : emailsResp.getBody()) {
                    if (emailNode.path("primary").asBoolean(false)
                            && emailNode.path("verified").asBoolean(false) == false) {
                        continue;
                    }
                    if (emailNode.path("primary").asBoolean(false)
                            && emailNode.path("verified").asBoolean(false)) {
                        email = emailNode.path("email").asText();
                        break;
                    }
                }
            }

            // 3) name 필드 우선, 없으면 login
            String login = userJson.path("login").asText();
            String name  = userJson.hasNonNull("name")
                    ? userJson.get("name").asText()
                    : login;

            // 4) 프로필 이미지
            String avatarUrl = userJson.path("avatar_url").asText("");

            return new GitHubUserInfo(
                    userJson.path("id").asLong(),
                    email,
                    name,
                    avatarUrl
            );
        } catch (Exception e) {
            log.error("GitHub 사용자 정보 조회 실패", e);
            throw new AuthException(ErrorStatus.AUTH_GITHUB_FAIL);
        }
    }
}