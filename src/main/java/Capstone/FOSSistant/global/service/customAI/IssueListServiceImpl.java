package Capstone.FOSSistant.global.service.customAI;

import Capstone.FOSSistant.global.aop.annotation.MeasureExecutionTime;
import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.exception.ClassificationException;
import Capstone.FOSSistant.global.converter.IssueListConverter;
import Capstone.FOSSistant.global.domain.enums.Tag;
import Capstone.FOSSistant.global.repository.IssueListRepository;
import Capstone.FOSSistant.global.service.GitHubHelperService;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IssueListServiceImpl implements IssueListService {

    private final IssueListRepository issueListRepository;
    private final StringRedisTemplate redisTemplate;
    private final GitHubHelperService githubHelperService;
    private final IssueListConverter issueListConverter;
    private final AIClassifierClient aiClassifierClient;

    //여러 이슈 분류 요청 (batch + sleep으로 AI 서버 부하 제어)
    @Override
    @MeasureExecutionTime
    public List<CompletableFuture<IssueListResponseDTO.IssueResponseDTO>> classifyAll(List<IssueListRequestDTO.IssueRequestDTO> dtoList) {
        long start = System.currentTimeMillis();

        int batchSize = 5;
        List<CompletableFuture<IssueListResponseDTO.IssueResponseDTO>> allFutures = new ArrayList<>();

        for (int i = 0; i < dtoList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dtoList.size());
            List<IssueListRequestDTO.IssueRequestDTO> batch = dtoList.subList(i, end);

            List<CompletableFuture<IssueListResponseDTO.IssueResponseDTO>> batchFutures =
                    batch.stream()
                            .map(this::classify)
                            .toList();

            allFutures.addAll(batchFutures);

            try {
                Thread.sleep(10); // AI 서버 부하 줄이기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long end = System.currentTimeMillis();
        log.info("[총 classifyAll 소요 시간] {}ms", (end - start));

        return allFutures;
    }


    //단건 이슈 분류
    @MeasureExecutionTime
    private CompletableFuture<IssueListResponseDTO.IssueResponseDTO> classify(IssueListRequestDTO.IssueRequestDTO dto) {
        String key = "issue:" + dto.getIssueId();

        return CompletableFuture.supplyAsync(() -> redisTemplate.opsForValue().get(key))
                .thenCompose(cached -> {
                    if (cached != null) {
                        Tag cachedTag = Tag.valueOf(cached.toUpperCase());
                        return CompletableFuture.completedFuture(issueListConverter.toResponseDTO(dto.getIssueId(), cachedTag));
                    }

                    return safeClassify(dto.getIssueId())
                            .thenApply(difficulty -> {
                                cacheDifficulty(key, difficulty);
                                persistDifficulty(dto, difficulty);
                                return issueListConverter.toResponseDTO(dto.getIssueId(), difficulty);
                            });
                });
    }

    //캐시 저장
    @MeasureExecutionTime
    private void cacheDifficulty(String key, Tag difficulty) {
        try {
            redisTemplate.opsForValue().set(key, difficulty.name().toLowerCase(), Duration.ofDays(7));
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 저장 실패: {}", e.getMessage());
        }
    }

    //DB 저장
    @MeasureExecutionTime
    private void persistDifficulty(IssueListRequestDTO.IssueRequestDTO dto, Tag difficulty) {
        try {
            var entity = issueListConverter.toEntity(dto, difficulty);
            issueListRepository.save(entity);
        } catch (DataAccessException e) {
            log.warn("DB 저장 실패: {}", e.getMessage());
        }
    }



    //이슈 URL로부터 title + body 추출 후 AI 분류
    private CompletableFuture<Tag> safeClassify(String issueUrl) {
        return CompletableFuture.supplyAsync(() -> extractTitleAndBody(issueUrl))
                .thenCompose(arr -> classifyWithAI(arr[0], arr[1]));
    }

    //GitHub API 호출 → 제목과 본문 추출
    @MeasureExecutionTime
    private String[] extractTitleAndBody(String issueUrl) {
        try {
            String[] parts = issueUrl.split("/");
            String owner = parts[3];
            String repo = parts[4];
            String issueNumber = parts[6];
            String[] titleAndBody = githubHelperService.fetchIssueData(owner, repo, issueNumber);
            String title = titleAndBody[0];
            String body = titleAndBody[1];
            return new String[]{title, body};
        } catch (Exception e) {
            throw new ClassificationException(ErrorStatus.AI_API_FAIL);
        }
    }

    //AI 분류 요청 → difficulty 추출
    public CompletableFuture<Tag> classifyWithAI(String title, String body) {
        long start = System.currentTimeMillis();

        return aiClassifierClient.classify(title, body)
                .map(result -> {
                    long end = System.currentTimeMillis();
                    log.info("[AI 분류 소요 시간] {}ms", (end - start));

                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        var root = objectMapper.readTree(result);
                        var results = root.get("results");

                        if (results != null && results.isArray() && results.size() > 0) {
                            var firstResult = results.get(0);
                            var difficultyNode = firstResult.get("difficulty");

                            if (difficultyNode != null && !difficultyNode.isNull()) {
                                String diff = difficultyNode.asText().toLowerCase();
                                return switch (diff) {
                                    case "easy" -> Tag.EASY;
                                    case "medium" -> Tag.MEDIUM;
                                    case "hard" -> Tag.HARD;
                                    default -> Tag.MISC;
                                };
                            }
                        }

                        log.warn("AI 응답에 유효한 difficulty가 없음 - result: {}", result);
                        return Tag.MISC;

                    } catch (Exception e) {
                        log.error("AI 응답 파싱 실패 - result: {}", result, e);
                        return Tag.MISC;
                    }
                })
                .toFuture();
    }

}
