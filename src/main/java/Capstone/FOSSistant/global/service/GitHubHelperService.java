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
    public record ContributeMeta(
            String contributingLink,
            List<String> issueTemplateLinks
    ) {}

    public ContributeMeta fetchContributionLinks(String owner, String repo) {
        long start = System.currentTimeMillis();

        String query = String.format("""
        query {
          repository(owner: "%s", name: "%s") {
            contributing: object(expression: "HEAD:CONTRIBUTING.md") {
              ... on Blob {
                id  # 존재 확인용
              }
            }
            issueTemplateDir: object(expression: "HEAD:.github/ISSUE_TEMPLATE") {
              ... on Tree {
                entries {
                  name
                  path
                  type
                }
              }
            }
          }
        }
    """, owner, repo);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("query", query), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    "https://api.github.com/graphql", HttpMethod.POST, request, JsonNode.class
            );

            JsonNode repoNode = response.getBody().path("data").path("repository");

            // CONTRIBUTING.md 링크
            boolean hasContributing = !repoNode.path("contributing").isMissingNode();
            String contributingLink = hasContributing
                    ? String.format("https://github.com/%s/%s/blob/HEAD/CONTRIBUTING.md", owner, repo)
                    : null;

            // ISSUE_TEMPLATE 링크 목록
            List<String> issueTemplateLinks = new ArrayList<>();
            JsonNode entries = repoNode.path("issueTemplateDir").path("entries");
            if (entries.isArray()) {
                for (JsonNode entry : entries) {
                    if ("blob".equals(entry.path("type").asText())) {
                        String path = entry.path("path").asText();
                        String url = String.format("https://github.com/%s/%s/blob/HEAD/%s", owner, repo, path);
                        issueTemplateLinks.add(url);
                    }
                }
            }

            long end = System.currentTimeMillis();
            log.info("[GitHub 기여 관련 링크 fetch 시간] {}ms", end - start);
            return new ContributeMeta(contributingLink, issueTemplateLinks);

        } catch (RestClientException e) {
            log.error("기여 관련 링크 조회 실패", e);
            throw new GithubApiException(ErrorStatus.GITHUB_API_FAIL);
        }
    }

    public record RepoMeta(String readme, String structure) {}

    public RepoMeta fetchRepoMetadata(String owner, String repo) {
        long start = System.currentTimeMillis();

        String query = String.format("""
        query {
          repository(owner: "%s", name: "%s") {
            readme: object(expression: "HEAD:README.md") {
              ... on Blob { text }
            }
            defaultBranchRef {
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
          }
        }
    """, owner, repo);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("query", query), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    "https://api.github.com/graphql", HttpMethod.POST, request, JsonNode.class
            );

            JsonNode repoNode = response.getBody().path("data").path("repository");
            String readme = repoNode.path("readme").path("text").asText("");
            StringBuilder structure = new StringBuilder();
            for (JsonNode node : repoNode
                    .path("defaultBranchRef").path("target")
                    .path("tree").path("entries")) {
                structure.append(node.path("path").asText()).append("\n");
            }

            long end = System.currentTimeMillis();
            log.info("[GitHub GraphQL repoMeta fetch 시간] {}ms", end - start);
            return new RepoMeta(readme, structure.toString().trim());
        } catch (RestClientException e) {
            log.error("Repo 메타데이터 조회 실패", e);
            throw new GithubApiException(ErrorStatus.GITHUB_API_FAIL);
        }
    }

    public record GitHubData(String title, String body, String readme, String structure) {}
}