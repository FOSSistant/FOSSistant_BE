package Capstone.FOSSistant.global.service;

import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.exception.GithubApiException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubHelperService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.github.token}")
    private String githubToken;

    @PostConstruct
    public void init() {
        githubToken = (githubToken == null ? "" : githubToken.trim());
        log.debug("GitHub token initialized: '{}'", githubToken.isEmpty() ? "[NONE]" : "[PROVIDED]");
    }

    public String fetchReadme(String owner, String repo) {
        String url = String.format("https://api.github.com/repos/%s/%s/readme", owner, repo);
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(true));

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return resp.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return "";
        } catch (RestClientException e) {
            throw new GithubApiException(ErrorStatus.GITHUB_API_FAIL);
        }
    }

    public String fetchRepoStructure(String owner, String repo) {
        String defaultBranch = fetchDefaultBranch(owner, repo);
        String url = String.format("https://api.github.com/repos/%s/%s/git/trees/%s?recursive=1",
                owner, repo, defaultBranch);
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(false));

        try {
            ResponseEntity<JsonNode> resp = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            List<String> paths = new ArrayList<>();
            for (JsonNode node : resp.getBody().path("tree")) {
                paths.add(node.path("path").asText());
            }
            return String.join("\n", paths);
        } catch (HttpClientErrorException.NotFound e) {
            return "";
        } catch (RestClientException e) {
            throw new GithubApiException(ErrorStatus.GITHUB_API_FAIL);
        }
    }

    private String fetchDefaultBranch(String owner, String repo) {
        String url = String.format("https://api.github.com/repos/%s/%s", owner, repo);
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(false));
        try {
            ResponseEntity<JsonNode> resp = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            JsonNode body = resp.getBody();
            return body.hasNonNull("default_branch")
                    ? body.get("default_branch").asText()
                    : "main";
        } catch (RestClientException e) {
            return "main";
        }
    }

    public String fetchIssueTitle(String owner, String repo, String issueNumber) {
        long start = System.currentTimeMillis();
        JsonNode issue = fetchIssueJson(owner, repo, issueNumber);
        long end = System.currentTimeMillis();
        log.info("[GitHub issue title fetch 시간] {}ms", (end - start));

        return issue.hasNonNull("title") ? issue.get("title").asText() : "";
    }

    public String fetchIssueBody(String owner, String repo, String issueNumber) {
        long start = System.currentTimeMillis();
        JsonNode issue = fetchIssueJson(owner, repo, issueNumber);
        long end = System.currentTimeMillis();
        log.info("[GitHub issue body fetch 시간] {}ms", (end - start));

        return issue.hasNonNull("body") ? issue.get("body").asText() : "";
    }

    private JsonNode fetchIssueJson(String owner, String repo, String issueNumber) {
        String url = String.format("https://api.github.com/repos/%s/%s/issues/%s", owner, repo, issueNumber);
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(false));
        try {
            ResponseEntity<JsonNode> resp = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return resp.getBody();
        } catch (HttpClientErrorException e) {
            log.error("GitHub API Error: {} – {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GithubApiException(ErrorStatus.GITHUB_API_FAIL);
        } catch (RestClientException e) {
            throw new GithubApiException(ErrorStatus.GITHUB_API_FAIL);
        }
    }

    private HttpHeaders createAuthHeaders(boolean rawReadme) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        headers.set("User-Agent", "FOSSistant/1.0");
        if (rawReadme) {
            headers.set("Accept", "application/vnd.github.v3.raw");
        } else {
            headers.set("Accept", "application/vnd.github.v3+json");
        }
        return headers;
    }
}