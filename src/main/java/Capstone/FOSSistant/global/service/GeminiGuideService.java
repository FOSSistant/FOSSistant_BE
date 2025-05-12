package Capstone.FOSSistant.global.service;

import Capstone.FOSSistant.global.config.AIConfig.GeminiChatClient;
import Capstone.FOSSistant.global.config.GitHubHelper;
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
    private final IssueListServiceImpl issueListService;

    public IssueGuideResponseDTO generateGuide(String issueUrl) {
        String[] parts = issueUrl.split("/");
        String owner = parts[3];
        String repo = parts[4];
        String issueNumber = parts[6];

        String title = githubHelper.fetchIssueTitle(owner, repo, issueNumber);
        String body = githubHelper.fetchIssueBody(owner, repo, issueNumber);
        String readme = githubHelper.fetchReadme(owner, repo);
        String structure = githubHelper.fetchRepoStructure(owner, repo);

        String prompt = promptBuilder.buildPrompt(title, body, readme, structure);

        IssueGuideResponseDTO geminiResponse = chatClient.call(prompt);

        String difficulty = issueListService.dummyClassify(title, body).name().toLowerCase();

        return IssueGuideResponseDTO.builder()
                .title(title)
                .difficulty(difficulty)
                .description(geminiResponse.getDescription())
                .solution(geminiResponse.getSolution())
                .caution(geminiResponse.getCaution())
                .build();
    }

}