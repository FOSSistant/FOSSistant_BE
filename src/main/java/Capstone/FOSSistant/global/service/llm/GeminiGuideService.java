package Capstone.FOSSistant.global.service.llm;

import Capstone.FOSSistant.global.aop.annotation.MeasureExecutionTime;
import Capstone.FOSSistant.global.service.GitHubHelperService;
import Capstone.FOSSistant.global.service.IssueListServiceImpl;
import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiGuideService {

    private final PromptBuilder promptBuilder;
    private final GeminiChatClient chatClient;
    private final GitHubHelperService githubHelperService;
    private final IssueListServiceImpl issueListService;

    @MeasureExecutionTime
    public CompletableFuture<IssueGuideResponseDTO> generateGuide(String issueUrl) {
        long totalStart = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            long githubStart = System.currentTimeMillis();

            String[] parts = issueUrl.split("/");
            String owner = parts[3];
            String repo = parts[4];
            String issueNumber = parts[6];

            GitHubHelperService.GitHubData data = githubHelperService.fetchAllData(owner, repo, issueNumber);
            String title = data.title();
            String body = data.body();
            String readme = data.readme();
            String structure = data.structure();
            long githubEnd = System.currentTimeMillis();
            log.info("[GitHub API 총 호출 시간] {}ms", (githubEnd - githubStart));

            long geminiStart = System.currentTimeMillis();

            String prompt = promptBuilder.buildPrompt(title, body, readme, structure);
            IssueGuideResponseDTO geminiResponse = chatClient.call(prompt);
            long geminiEnd = System.currentTimeMillis();
            log.info("[Gemini 응답 처리 시간] {}ms", (geminiEnd - geminiStart));

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
        ).whenComplete((res, ex) -> {
            long totalEnd = System.currentTimeMillis();
            if (ex != null) {
                log.error("[generateGuide 실패] 에러: {}", ex.getMessage(), ex);
            }
            log.info("[generateGuide 전체 소요 시간] {}ms", (totalEnd - totalStart));
        });
    }
}