package Capstone.FOSSistant.global.service;

import Capstone.FOSSistant.global.aop.annotation.MeasureExecutionTime;
import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.exception.ClassificationException;
import Capstone.FOSSistant.global.converter.IssueListConverter;
import Capstone.FOSSistant.global.domain.enums.Tag;
import Capstone.FOSSistant.global.repository.IssueListRepository;
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

    @Override
    @MeasureExecutionTime
    public CompletableFuture<IssueListResponseDTO.IssueResponseDTO> classify(IssueListRequestDTO.IssueRequestDTO dto) {
        String key = "issue:" + dto.getIssueId();

        return CompletableFuture.supplyAsync(() -> redisTemplate.opsForValue().get(key))
                .thenCompose(cached -> {
                    if (cached != null) {
                        return CompletableFuture.completedFuture(issueListConverter.toResponseDTO(
                                dto.getIssueId(), Tag.valueOf(cached.toUpperCase())));
                    }

                    return safeClassify(dto.getIssueId())
                            .thenApply(difficulty -> {
                                try {
                                    redisTemplate.opsForValue().set(key, difficulty.name().toLowerCase(), Duration.ofDays(7));
                                } catch (RedisConnectionFailureException e) {
                                    log.warn("Redis 저장 실패: {}", e.getMessage());
                                }

                                try {
                                    var entity = issueListConverter.toEntity(dto, difficulty);
                                    issueListRepository.save(entity);
                                } catch (DataAccessException e) {
                                    log.warn("DB 저장 실패: {}", e.getMessage());
                                }

                                return issueListConverter.toResponseDTO(dto.getIssueId(), difficulty);
                            });
                });
    }

    @MeasureExecutionTime
    public CompletableFuture<Tag> safeClassify(String issueUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String[] parts = issueUrl.split("/");
                String owner = parts[3];
                String repo = parts[4];
                String issueNumber = parts[6];
                String title = githubHelperService.fetchIssueTitle(owner, repo, issueNumber);
                String body = githubHelperService.fetchIssueBody(owner, repo, issueNumber);
                return new String[]{title, body};
            } catch (Exception e) {
                throw new ClassificationException(ErrorStatus.AI_API_FAIL);
            }
        }).thenCompose(arr -> classifyWithAI(arr[0], arr[1]));
    }

    public CompletableFuture<Tag> classifyWithAI(String title, String body) {
        return aiClassifierClient.classify(title, body)
                .map(result -> {
                    try {
                        String diff = new ObjectMapper().readTree(result).get("difficulty").asText().toLowerCase();
                        return switch (diff) {
                            case "easy" -> Tag.EASY;
                            case "medium" -> Tag.MEDIUM;
                            case "hard" -> Tag.HARD;
                            default -> Tag.MISC;
                        };
                    } catch (Exception e) {
                        log.error("응답 파싱 실패", e);
                        return Tag.MISC;
                    }
                })
                .toFuture();
    }

    @MeasureExecutionTime
    public List<CompletableFuture<IssueListResponseDTO.IssueResponseDTO>> classifyAll(List<IssueListRequestDTO.IssueRequestDTO> dtoList) {
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
                Thread.sleep(500); // AI 서버 부하 줄이기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return allFutures;
    }
}
