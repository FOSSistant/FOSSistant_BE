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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
    private final ThreadPoolTaskExecutor classifierExecutor; // 커스텀 풀

    /**
     * 이슈 가이드를 생성합니다. 이미 저장된 이슈가 있으면 즉시 반환,
     * 아니면 GitHub → LLM → 분류 → 저장 흐름을 비동기로 수행합니다.
     */
    @MeasureExecutionTime
    public CompletableFuture<IssueGuideResponseDTO> generateGuide(String issueUrl) {
        long totalStart = System.currentTimeMillis();

        // (1) DB 조회: IssueDetail이 이미 존재하는지 비동기 검사
        return CompletableFuture.supplyAsync(() ->
                        issueDetailRepository.findByIssue_Id(issueUrl).orElse(null),
                classifierExecutor
        ).thenComposeAsync(saved -> {
            if (saved != null) {
                // 이미 있으면 즉시 DTO 반환
                return CompletableFuture.completedFuture(convertToDTO(saved));
            }
            // 없으면 GitHub → LLM → processGeminiResponse 비동기로 처리
            return CompletableFuture.supplyAsync(() -> fetchAllData(issueUrl), classifierExecutor)
                    .thenComposeAsync(this::processGeminiResponse, classifierExecutor);
        }, classifierExecutor).whenComplete((res, ex) -> {
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

    /**
     * GitHub 호출, Redis 캐시, LLM 호출을 모두 블로킹 -> classifierExecutor 풀에서 실행
     */
    private Object[] fetchAllData(String issueUrl) {
        // 1) URL 파싱
        String[] parts = issueUrl.split("/");
        String owner = parts[3];
        String repo = parts[4];
        String issueNumber = parts[6];

        // 2) GitHub에서 컨트리뷰션 메타 가져오기
        GitHubHelperService.ContributeMeta contribMeta = githubHelperService.fetchContributionLinks(owner, repo);
        String relatedLinks = buildRelatedLinks(contribMeta);
        List<String> links = extractLinks(contribMeta);

        // 3) Redis에서 README/구조 캐시 확인
        String readmeKey = "repo:readme:" + owner + "/" + repo;
        String structureKey = "repo:structure:" + owner + "/" + repo;
        String readme = redisTemplate.opsForValue().get(readmeKey);
        String structure = redisTemplate.opsForValue().get(structureKey);

        if (readme == null || structure == null) {
            GitHubHelperService.RepoMeta meta = githubHelperService.fetchRepoMetadata(owner, repo);
            if (readme == null) {
                redisTemplate.opsForValue().set(readmeKey, meta.readme(), Duration.ofDays(1));
                readme = meta.readme();
            }
            if (structure == null) {
                redisTemplate.opsForValue().set(structureKey, meta.structure(), Duration.ofDays(1));
                structure = meta.structure();
            }
        }

        // 4) GitHub에서 이슈 제목/본문 추출
        String[] issueData = githubHelperService.fetchIssueData(owner, repo, issueNumber);
        String title = issueData[0];
        String body = issueData[1];

        // 5) LLM 호출(buildPrompt → chatClient.call)
        String prompt = promptBuilder.buildPrompt(title, body, readme, structure, links);
        IssueGuideResponseDTO geminiResponse = chatClient.call(prompt);

        return new Object[]{issueUrl, title, body, geminiResponse, relatedLinks};
    }

    /**
     * LLM 결과를 받아 Redis 캐시 또는 AI 분류 서비스로 태그를 결정한 뒤,
     * DB에 저장하고 DTO를 반환하는 메서드. 분류는 비동기로 수행
     */
    @Transactional
    public CompletableFuture<IssueGuideResponseDTO> processGeminiResponse(Object[] arr) {
        String issueId = (String) arr[0];
        String title = (String) arr[1];
        String body = (String) arr[2];
        IssueGuideResponseDTO gemini = (IssueGuideResponseDTO) arr[3];
        String relatedLinks = (String) arr[4];

        // 1) Redis 캐시된 태그 확인
        String tagCached = redisTemplate.opsForValue().get("issue:" + issueId);
        if (tagCached != null) {
            Tag tag = Tag.valueOf(tagCached.toUpperCase());
            // 이미 분류되어 있다면 즉시 저장(업데이트) 없이 DTO 반환
            return CompletableFuture.completedFuture(saveDetail(issueId, title, tag, gemini, relatedLinks));
        }

        // 2) 미분류 상태면 AI 분류 서비스로 태그 결정 (비동기)

        return issueListService.classifyWithAI(title, body)
                .thenApplyAsync(diffRes ->
                                saveDetail(
                                        issueId,
                                        title,
                                        diffRes.tag(),      // 태그는 .tag()
                                        gemini,
                                        relatedLinks
                                ),
                        classifierExecutor);
    }

    /**
     * DB에 이슈 상세 정보를 저장하고, 저장된 엔티티를 기반으로 DTO를 생성하여 반환
     */
    @Transactional(rollbackFor = Exception.class)
    public IssueGuideResponseDTO saveDetail(String issueId,
                                            String title,
                                            Tag tag,
                                            IssueGuideResponseDTO gemini,
                                            String relatedLinks) {
        // 1) IssueList 테이블에 difficulty가 없으면 새로 저장
        IssueList issue = issueListRepository.findById(issueId)
                .orElseGet(() -> issueListRepository.save(IssueList.builder()
                        .id(issueId)
                        .difficulty(tag)
                        .build()));

        // 2) IssueDetail 엔티티 생성 및 저장
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

        // 3) 저장된 엔티티를 DTO로 변환하여 반환
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

    /**
     * 사용자의 피드백을 저장(업서트)합니다.
     */
    @Transactional
    public void upsertFeedback(Member member, String issueId, Tag feedbackTag) {
        if (member == null || member.getMemberId() == null) {
            throw new MemberException(MEMBER_NOT_FOUND);
        }

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

    /**
     * 사용자가 남긴 feedback 태그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Tag getMyFeedback(Member member, String issueId) {
        return feedbackRepository.findByMemberAndIssue_Id(member, issueId)
                .map(IssueFeedback::getFeedbackTag)
                .orElse(null);
    }
}