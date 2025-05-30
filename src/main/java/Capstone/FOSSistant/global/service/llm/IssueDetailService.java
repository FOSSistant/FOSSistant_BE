package Capstone.FOSSistant.global.service.llm;

import Capstone.FOSSistant.global.aop.annotation.MeasureExecutionTime;
import Capstone.FOSSistant.global.apiPayload.exception.MemberException;
import Capstone.FOSSistant.global.domain.entity.IssueDetail;
import Capstone.FOSSistant.global.domain.entity.IssueFeedback;
import Capstone.FOSSistant.global.domain.entity.IssueList;
import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.domain.enums.Tag;
import Capstone.FOSSistant.global.repository.IssueDetailRepository;
import Capstone.FOSSistant.global.repository.IssueFeedbackRepository;
import Capstone.FOSSistant.global.repository.IssueListRepository;
import Capstone.FOSSistant.global.service.GitHubHelperService;
import Capstone.FOSSistant.global.service.customAI.IssueListServiceImpl;
import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus.MEMBER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueDetailService {

    private final PromptBuilder promptBuilder;
    private final GeminiChatClient chatClient;
    private final GitHubHelperService githubHelperService;
    private final IssueListServiceImpl issueListService;
    private final StringRedisTemplate redisTemplate;
    private final IssueFeedbackRepository feedbackRepository;
    private final IssueListRepository issueListRepository;
    private final IssueDetailRepository issueDetailRepository;

    @MeasureExecutionTime
    public CompletableFuture<IssueGuideResponseDTO> generateGuide(String issueUrl) {
        long totalStart = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            return issueDetailRepository.findByIssue_Id(issueUrl).orElse(null);
        }).thenCompose(saved -> {
            if (saved != null) {
                return CompletableFuture.completedFuture(convertToDTO(saved));
            }
            return CompletableFuture.supplyAsync(() -> fetchAllData(issueUrl))
                    .thenCompose(this::processGeminiResponse);
        }).whenComplete((res, ex) -> {
            if (ex != null) {
                log.error("[generateGuide 에러 발생]: {}", ex.getMessage(), ex);
            }
            log.info("[전체 generateGuide 처리 시간] {}ms", System.currentTimeMillis() - totalStart);
        });
    }

    private IssueGuideResponseDTO convertToDTO(IssueDetail saved) {
        return IssueGuideResponseDTO.builder()
                .title(saved.getTitle())
                .difficulty(saved.getTag().name().toLowerCase())
                .description(saved.getDescription())
                .solution(saved.getSolution())
                .highlightedBody(saved.getHighlightedBody())
                .relatedLinks(saved.getRelatedLinks())
                .build();
    }

    private Object[] fetchAllData(String issueUrl) {
        String[] parts = issueUrl.split("/");
        String owner = parts[3];
        String repo = parts[4];
        String issueNumber = parts[6];

        GitHubHelperService.ContributeMeta contribMeta = githubHelperService.fetchContributionLinks(owner, repo);
        String relatedLinks = buildRelatedLinks(contribMeta);
        List<String> links = extractLinks(contribMeta);

        String readmeKey = "repo:readme:" + owner + "/" + repo;
        String structureKey = "repo:structure:" + owner + "/" + repo;

        String readme = redisTemplate.opsForValue().get(readmeKey);
        String structure = redisTemplate.opsForValue().get(structureKey);

        if (readme == null || structure == null) {
            GitHubHelperService.RepoMeta meta = githubHelperService.fetchRepoMetadata(owner, repo);
            if (readme == null) redisTemplate.opsForValue().set(readmeKey, readme = meta.readme(), Duration.ofDays(1));
            if (structure == null) redisTemplate.opsForValue().set(structureKey, structure = meta.structure(), Duration.ofDays(1));
        }

        String[] issueData = githubHelperService.fetchIssueData(owner, repo, issueNumber);
        String title = issueData[0];
        String body = issueData[1];
        String prompt = promptBuilder.buildPrompt(title, body, readme, structure, links);
        IssueGuideResponseDTO geminiResponse = chatClient.call(prompt);

        return new Object[]{issueUrl, title, body, geminiResponse, relatedLinks};
    }

    @Transactional
    public CompletableFuture<IssueGuideResponseDTO> processGeminiResponse(Object[] arr) {
        String issueId = (String) arr[0];
        String title = (String) arr[1];
        String body = (String) arr[2];
        IssueGuideResponseDTO gemini = (IssueGuideResponseDTO) arr[3];
        String relatedLinks = (String) arr[4];

        String tagCached = redisTemplate.opsForValue().get("issue:" + issueId);
        if (tagCached != null) {
            Tag tag = Tag.valueOf(tagCached.toUpperCase());
            return CompletableFuture.completedFuture(saveDetail(issueId, title, tag, gemini, relatedLinks));
        }

        return issueListService.classifyWithAI(title, body)
                .thenApply(tag -> saveDetail(issueId, title, tag, gemini, relatedLinks));
    }

    @Transactional
    public IssueGuideResponseDTO saveDetail(String issueId, String title, Tag tag, IssueGuideResponseDTO gemini, String relatedLinks) {
        IssueList issue = issueListRepository.findById(issueId)
                .orElseGet(() -> issueListRepository.save(IssueList.builder()
                        .id(issueId)
                        .difficulty(tag)
                        .build()));

        IssueDetail detail = IssueDetail.builder()
                .issue(issue)
                .title(title)
                .description(gemini.getDescription())
                .solution(gemini.getSolution())
                .highlightedBody(gemini.getHighlightedBody())
                .relatedLinks(relatedLinks)
                .build();

        issueDetailRepository.save(detail);

        log.info("[LLM 결과 DB 저장 완료]: {}", issueId);
        return convertToDTO(detail);
    }

    private String buildRelatedLinks(GitHubHelperService.ContributeMeta contribMeta) {
        StringBuilder sb = new StringBuilder();
        if (contribMeta.contributingLink() != null) {
            sb.append("- [CONTRIBUTING.md](").append(contribMeta.contributingLink()).append(")\n");
        }
        for (String link : contribMeta.issueTemplateLinks()) {
            sb.append("- [Issue Template](").append(link).append(")\n");
        }
        return sb.toString().trim();
    }

    private List<String> extractLinks(GitHubHelperService.ContributeMeta contribMeta) {
        List<String> links = new ArrayList<>();
        if (contribMeta.contributingLink() != null) {
            links.add(String.format("- [CONTRIBUTING.md](%s)", contribMeta.contributingLink()));
        }
        for (String link : contribMeta.issueTemplateLinks()) {
            links.add(String.format("- [Issue Template](%s)", link));
        }
        return links;
    }

    @Transactional
    public void upsertFeedback(Member member, String issueId, Tag feedbackTag) {
        if (member == null || member.getMemberId() == null) throw new MemberException(MEMBER_NOT_FOUND);

        IssueList issue = issueListRepository.findById(issueId)
                .orElseGet(() -> issueListRepository.save(IssueList.builder()
                        .id(issueId)
                        .difficulty(Tag.MISC)
                        .build()));

        IssueFeedback fb = feedbackRepository.findByMemberAndIssue(member, issue)
                .orElseGet(() -> IssueFeedback.builder().member(member).issue(issue).build());

        fb.setFeedbackTag(feedbackTag);
        feedbackRepository.save(fb);
    }

    @Transactional(readOnly = true)
    public Tag getMyFeedback(Member member, String issueId) {
        return feedbackRepository.findByMemberAndIssue_Id(member, issueId)
                .map(IssueFeedback::getFeedbackTag)
                .orElse(null);
    }
}
