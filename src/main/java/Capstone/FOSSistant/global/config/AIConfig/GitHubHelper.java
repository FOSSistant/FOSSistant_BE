package Capstone.FOSSistant.global.config.AIConfig;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GitHubHelper {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String GITHUB_API = "https://api.github.com/repos/";

    public String fetchReadme(String owner, String repo) {
        String url = GITHUB_API + owner + "/" + repo + "/readme";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3.raw");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return response.getBody();
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