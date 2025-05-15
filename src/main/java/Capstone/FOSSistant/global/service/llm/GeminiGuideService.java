package Capstone.FOSSistant.global.service.llm;

import Capstone.FOSSistant.global.service.GitHubHelperService;
import Capstone.FOSSistant.global.service.IssueListServiceImpl;
import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

//@Service
//@RequiredArgsConstructor
//public class GeminiGuideService {
//
//    private final PromptBuilder promptBuilder;
//    private final GeminiChatClient chatClient;
//    private final GitHubHelperService githubHelperService;
//    private final IssueListServiceImpl issueListService;
//
//    public IssueGuideResponseDTO generateGuide(String issueUrl) {
//        String[] parts = issueUrl.split("/");
//        String owner = parts[3];
//        String repo = parts[4];
//        String issueNumber = parts[6];
//
//        String title = githubHelperService.fetchIssueTitle(owner, repo, issueNumber);
//        String body = githubHelperService.fetchIssueBody(owner, repo, issueNumber);
//        String readme = githubHelperService.fetchReadme(owner, repo);
//        String structure = githubHelperService.fetchRepoStructure(owner, repo);
//
//        String prompt = promptBuilder.buildPrompt(title, body, readme, structure);
//
//        IssueGuideResponseDTO geminiResponse = chatClient.call(prompt);
//
//        String difficulty = issueListService.classifyWithAI(title, body).name().toLowerCase();
//
//        return IssueGuideResponseDTO.builder()
//                .title(title)
//                .difficulty(difficulty)
//                .description(geminiResponse.getDescription())
//                .solution(geminiResponse.getSolution())
//                .caution(geminiResponse.getCaution())
//                .build();
//    }
//
//}


import Capstone.FOSSistant.global.service.GitHubHelperService;
import Capstone.FOSSistant.global.service.IssueListServiceImpl;
import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GeminiGuideService {

    private final PromptBuilder promptBuilder;
    private final GeminiChatClient chatClient;
    private final GitHubHelperService githubHelperService;
    private final IssueListServiceImpl issueListService;

    public CompletableFuture<IssueGuideResponseDTO> generateGuide(String issueUrl) {
        return CompletableFuture.supplyAsync(() -> {
            String[] parts = issueUrl.split("/");
            String owner = parts[3];
            String repo = parts[4];
            String issueNumber = parts[6];

            String title = githubHelperService.fetchIssueTitle(owner, repo, issueNumber);
            String body = githubHelperService.fetchIssueBody(owner, repo, issueNumber);
            String readme = githubHelperService.fetchReadme(owner, repo);
            String structure = githubHelperService.fetchRepoStructure(owner, repo);

            String prompt = promptBuilder.buildPrompt(title, body, readme, structure);
            IssueGuideResponseDTO geminiResponse = chatClient.call(prompt);

            // 이 title/body를 사용해 classifyWithAI를 비동기적으로 호출
            return new String[]{title, body, geminiResponse.getDescription(), geminiResponse.getSolution(), geminiResponse.getCaution()};
        }).thenCompose(arr ->
                issueListService.classifyWithAI(arr[0], arr[1])
                        .thenApply(tag ->
                                IssueGuideResponseDTO.builder()
                                        .title(arr[0])
                                        .difficulty(tag.name().toLowerCase())
                                        .description(arr[2])
                                        .solution(arr[3])
                                        .caution(arr[4])
                                        .build()
                        )
        );
    }
}