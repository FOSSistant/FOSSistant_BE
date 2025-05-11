package Capstone.FOSSistant.global.config.AIConfig;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GitHubHelper {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String GITHUB_API = "https://api.github.com/repos/";

    public String fetchReadme(String owner, String repo) {
        try {
            String url = "https://api.github.com/repos/" + owner + "/" + repo + "/readme";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.github.v3.raw");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            // README가 없을 경우 빈 값 반환
            return "";
        } catch (Exception e) {
            throw new RuntimeException("README 가져오기 실패", e);
        }
    }

    public String fetchRepoStructure(String owner, String repo) {
        String url = GITHUB_API + owner + "/" + repo + "/git/trees/main?recursive=1";
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        List<String> files = new ArrayList<>();
        for (JsonNode node : response.getBody().path("tree")) {
            files.add(node.path("path").asText());
        }

        return String.join("\n", files);
    }
}