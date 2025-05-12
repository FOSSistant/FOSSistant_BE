package Capstone.FOSSistant.global.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GitHubHelper {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.github.token}")
    private String githubToken;

    public String fetchReadme(String owner, String repo) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/readme";

        HttpHeaders headers = createAuthHeaders();
        headers.set("Accept", "application/vnd.github.v3.raw");  // README 원본

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return ""; // README 없음
        } catch (Exception e) {
            throw new RuntimeException("README 요청 실패: " + e.getMessage(), e);
        }
    }

    public String fetchRepoStructure(String owner, String repo) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/git/trees/main?recursive=1";

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            List<String> paths = new ArrayList<>();
            for (JsonNode node : response.getBody().path("tree")) {
                paths.add(node.path("path").asText());
            }

            return String.join("\n", paths);

        } catch (Exception e) {
            throw new RuntimeException("Repo 구조 요청 실패: " + e.getMessage(), e);
        }
    }

    public String fetchIssueTitle(String owner, String repo, String issueNumber) {
        JsonNode issue = fetchIssueJson(owner, repo, issueNumber);
        return issue.hasNonNull("title") ? issue.get("title").asText() : "";
    }

    public String fetchIssueBody(String owner, String repo, String issueNumber) {
        JsonNode issue = fetchIssueJson(owner, repo, issueNumber);
        return issue.hasNonNull("body") ? issue.get("body").asText() : "";
    }

    private JsonNode fetchIssueJson(String owner, String repo, String issueNumber) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/issues/" + issueNumber;

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("이슈 정보 요청 실패: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}