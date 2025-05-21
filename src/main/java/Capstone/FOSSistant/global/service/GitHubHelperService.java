package Capstone.FOSSistant.global.service;

import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.exception.GithubApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubHelperService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Value("${spring.github.token}")
    private String githubToken;

    @PostConstruct
    public void init() {
        githubToken = (githubToken == null ? "" : githubToken.trim());
        log.debug("GitHub token initialized: '{}'", githubToken.isEmpty() ? "[NONE]" : "[PROVIDED]");
    }

    public String[] fetchIssueData(String owner, String repo, String issueNumber) {
        long start = System.currentTimeMillis();

        String query = String.format("""
                query {
                  repository(owner: \"%s\", name: \"%s\") {
                    issue(number: %s) {
                      title
                      body
                    }
                  }
                }
                """, owner, repo, issueNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("query", query), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    "https://api.github.com/graphql",
                    HttpMethod.POST,
                    request,
                    JsonNode.class
            );

            JsonNode issueNode = response.getBody()
                    .path("data")
                    .path("repository")
                    .path("issue");

            String title = issueNode.path("title").asText("");
            String body = issueNode.path("body").asText("");

            long end = System.currentTimeMillis();
            log.info("[GitHub GraphQL 이슈 fetch 시간] {}ms", (end - start));

            return new String[]{title, body};

        } catch (RestClientException e) {
            log.error("GitHub GraphQL API 호출 실패", e);
            throw new GithubApiException(ErrorStatus.GITHUB_API_FAIL);
        }
    }

    public GitHubData fetchAllData(String owner, String repo, String issueNumber) {
        long start = System.currentTimeMillis();

        String query = String.format("""
        query {
          repository(owner: "%s", name: "%s") {
            issue(number: %s) {
              title
              body
            }
            defaultBranchRef {
              name
              target {
                ... on Commit {
                  tree {
                    entries {
                      path
                    }
                  }
                }
              }
            }
            readme: object(expression: "HEAD:README.md") {
              ... on Blob {
                text
              }
            }
          }
        }
        """, owner, repo, issueNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("query", query), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    "https://api.github.com/graphql",
                    HttpMethod.POST,
                    request,
                    JsonNode.class
            );

            JsonNode repository = response.getBody().path("data").path("repository");

            String title = repository.path("issue").path("title").asText("");
            String body = repository.path("issue").path("body").asText("");
            String readme = repository.path("readme").path("text").asText("");

            StringBuilder structure = new StringBuilder();
            for (JsonNode node : repository
                    .path("defaultBranchRef")
                    .path("target")
                    .path("tree")
                    .path("entries")) {
                structure.append(node.path("path").asText()).append("\n");
            }

            long end = System.currentTimeMillis();
            log.info("[GitHub GraphQL 전체 데이터 fetch 시간] {}ms", (end - start));

            return new GitHubData(title, body, readme, structure.toString().trim());

        } catch (RestClientException e) {
            log.error("GitHub GraphQL API 호출 실패", e);
            throw new GithubApiException(ErrorStatus.GITHUB_API_FAIL);
        }
    }

    public record GitHubData(String title, String body, String readme, String structure) {}
}