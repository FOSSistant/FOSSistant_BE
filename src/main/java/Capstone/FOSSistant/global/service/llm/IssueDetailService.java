package Capstone.FOSSistant.global.service.llm;

import Capstone.FOSSistant.global.aop.annotation.MeasureExecutionTime;
import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.exception.GeneralException;
import Capstone.FOSSistant.global.apiPayload.exception.MemberException;
import Capstone.FOSSistant.global.domain.entity.IssueFeedback;
import Capstone.FOSSistant.global.domain.entity.IssueList;
import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.domain.enums.Tag;
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
import java.util.concurrent.CompletableFuture;

import static Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueDetailService {

    private final PromptBuilder promptBuilder;
    private final GeminiChatClient chatClient;
    private final GitHubHelperService githubHelperService;
    private final IssueListServiceImpl issueListService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final IssueFeedbackRepository feedbackRepository;
    private final IssueListRepository issueListRepository;


    @MeasureExecutionTime
    public CompletableFuture<IssueGuideResponseDTO> generateGuide(String issueUrl) {
        long totalStart = System.currentTimeMillis();
        String issueKey = "llm:guide:" + issueUrl;

        // 1. LLM 응답 캐싱 여부 확인
        String cached = redisTemplate.opsForValue().get(issueKey);
        if (cached != null) {
            try {
                log.info("[캐시된 LLM 응답 반환]");
                IssueGuideResponseDTO cachedDTO = objectMapper.readValue(cached, IssueGuideResponseDTO.class);
                return CompletableFuture.completedFuture(cachedDTO);
            } catch (Exception e) {
                log.warn("캐시 파싱 실패 → 재요청");
            }
        }

        return CompletableFuture.supplyAsync(() -> {
            long githubStart = System.currentTimeMillis();

            String[] parts = issueUrl.split("/");
            String owner = parts[3];
            String repo = parts[4];
            String issueNumber = parts[6];

            // repo 단위 캐시
            String readmeKey = "repo:readme:" + owner + "/" + repo;
            String structureKey = "repo:structure:" + owner + "/" + repo;

            String readme = redisTemplate.opsForValue().get(readmeKey);
            String structure = redisTemplate.opsForValue().get(structureKey);

            if (readme == null || structure == null) {
                GitHubHelperService.RepoMeta meta = githubHelperService.fetchRepoMetadata(owner, repo);
                if (readme == null) {
                    readme = meta.readme();
                    redisTemplate.opsForValue().set(readmeKey, readme, Duration.ofDays(1));
                }
                if (structure == null) {
                    structure = meta.structure();
                    redisTemplate.opsForValue().set(structureKey, structure, Duration.ofDays(1));
                }
            }

            String[] issueData = githubHelperService.fetchIssueData(owner, repo, issueNumber);
            String title = issueData[0];
            String body = issueData[1];

            long githubEnd = System.currentTimeMillis();
            log.info("[GitHub 데이터 로딩 시간] {}ms", githubEnd - githubStart);

            long geminiStart = System.currentTimeMillis();
            String prompt = promptBuilder.buildPrompt(title, body, readme, structure);
            IssueGuideResponseDTO geminiResponse = chatClient.call(prompt);
            long geminiEnd = System.currentTimeMillis();
            log.info("[Gemini 호출 및 응답 파싱 시간] {}ms", geminiEnd - geminiStart);

            return new Object[]{issueUrl, title, body, geminiResponse};
        }).thenCompose(arr -> {
            String issueId = (String) arr[0];
            String title = (String) arr[1];
            String body = (String) arr[2];
            IssueGuideResponseDTO gemini = (IssueGuideResponseDTO) arr[3];

            long aiStart = System.currentTimeMillis();
            String tagCached = redisTemplate.opsForValue().get("issue:" + issueId);
            if (tagCached != null) {
                Tag tag = Tag.valueOf(tagCached.toUpperCase());
                log.info("[AI 분류 캐시 HIT] 시간: {}ms", System.currentTimeMillis() - aiStart);
                return CompletableFuture.completedFuture(
                        buildAndCacheLLMResponse(issueId, title, tag, gemini)
                );
            }

            return issueListService.classifyWithAI(title, body)
                    .thenApply(tag -> {
                        redisTemplate.opsForValue().set("issue:" + issueId, tag.name().toLowerCase(), Duration.ofDays(7));
                        log.info("[AI 분류 수행 시간] {}ms", System.currentTimeMillis() - aiStart);
                        return buildAndCacheLLMResponse(issueId, title, tag, gemini);
                    });
        }).whenComplete((res, ex) -> {
            long totalEnd = System.currentTimeMillis();
            if (ex != null) {
                log.error("[generateGuide 에러 발생]: {}", ex.getMessage(), ex);
            }
            log.info("[전체 generateGuide 처리 시간] {}ms", totalEnd - totalStart);
        });
    }

    private IssueGuideResponseDTO buildAndCacheLLMResponse(String issueId, String title, Tag tag, IssueGuideResponseDTO gemini) {
        IssueGuideResponseDTO finalDTO = IssueGuideResponseDTO.builder()
                .title(title)
                .difficulty(tag.name().toLowerCase())
                .description(gemini.getDescription())
                .solution(gemini.getSolution())
                .caution(gemini.getCaution())
                .build();

        try {
            String json = objectMapper.writeValueAsString(finalDTO);
            redisTemplate.opsForValue().set("llm:guide:" + issueId, json, Duration.ofHours(6));
        } catch (Exception e) {
            log.warn("LLM 결과 캐싱 실패: {}", e.getMessage());
        }

        return finalDTO;
    }

    @Transactional
    public void upsertFeedback(Member member, String issueId, Tag feedbackTag) {
        if (member == null || member.getMemberId() == null) {
            throw new MemberException(MEMBER_NOT_FOUND);
        }

        IssueList issue = issueListRepository.findById(issueId)
                .orElseGet(() -> issueListRepository.save(
                        IssueList.builder()
                                .id(issueId)
                                .difficulty(Tag.MISC)
                                .build()
                ));

        IssueFeedback fb = feedbackRepository
                .findByMemberAndIssue(member, issue)
                .orElseGet(() -> {
                    IssueFeedback newFb = IssueFeedback.builder()
                            .member(member)
                            .issue(issue)
                            .build();
                    return newFb;
                });

        fb.setFeedbackTag(feedbackTag);
        feedbackRepository.save(fb);
    }
}