package Capstone.FOSSistant.global.service;

import Capstone.FOSSistant.global.config.AIConfig.GeminiChatClient;
import Capstone.FOSSistant.global.config.AIConfig.GitHubHelper;
import Capstone.FOSSistant.global.config.AIConfig.PromptBuilder;
import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiGuideService {

    private final PromptBuilder promptBuilder;
    private final GeminiChatClient chatClient;
    private final GitHubHelper githubHelper;

    public IssueGuideResponseDTO generateGuide(String issueUrl, String title, String body) {
        String[] parts = issueUrl.split("/");
        String owner = parts[3];
        String repo = parts[4];

        String readme = githubHelper.fetchReadme(owner, repo);
        String structure = githubHelper.fetchRepoStructure(owner, repo);
        String prompt = promptBuilder.buildPrompt(title, body, readme, structure);

        return chatClient.call(prompt);
    }
}