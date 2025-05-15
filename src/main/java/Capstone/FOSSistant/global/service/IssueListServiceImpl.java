package Capstone.FOSSistant.global.service;

import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.exception.ClassificationException;
import Capstone.FOSSistant.global.apiPayload.exception.GithubApiException;
import Capstone.FOSSistant.global.apiPayload.exception.RedisConnectionException;
import Capstone.FOSSistant.global.converter.IssueListConverter;
import Capstone.FOSSistant.global.domain.enums.Tag;
import Capstone.FOSSistant.global.repository.IssueListRepository;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    public CompletableFuture<IssueListResponseDTO.IssueResponseDTO> classify(IssueListRequestDTO.IssueRequestDTO dto) {
        return CompletableFuture.supplyAsync(() -> {
            String key = "issue:" + dto.getIssueId();
            Tag difficulty;

            // Redis 조회
            try {
                String cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    log.info("캐시 히트: {}", key);
                    return issueListConverter.toResponseDTO(dto.getIssueId(),
                            Tag.valueOf(cached.toUpperCase()));
                }
            } catch (RedisConnectionFailureException e) {
                log.warn("Redis 장애—폴백: {}", e.getMessage());
                throw new RedisConnectionException(ErrorStatus.REDIS_CONNECTION_FAIL);
            }

            //실제 분류
            difficulty = safeClassify(dto.getIssueId());

            // 캐시 저장
            try {
                redisTemplate.opsForValue().set(key, difficulty.name().toLowerCase(), Duration.ofDays(7));
            } catch (RedisConnectionFailureException e) {
                log.warn("Redis 저장 실패: {}", e.getMessage());
            }

            // DB 저장
            try {
                var result = issueListConverter.toEntity(dto,difficulty);
                issueListRepository.save(result);
            } catch (DataAccessException e) {
                log.warn("DB 저장 실패: {}", e.getMessage());
            }

            return issueListConverter.toResponseDTO(dto.getIssueId(), difficulty);
        });
    }

    private Tag safeClassify(String issueUrl) {
        try {
            log.debug("classify 시작: {}", issueUrl);
            // 이슈 URL → owner, repo, issueNumber 추출
            String[] parts = issueUrl.split("/");
            String owner = parts[3];
            String repo = parts[4];
            String issueNumber = parts[6];

            // GitHub API로 title/body 가져오기
            String title = githubHelperService.fetchIssueTitle(owner, repo, issueNumber);
            String body = githubHelperService.fetchIssueBody(owner, repo, issueNumber);

            String shortBody = body.replaceAll("(?s)```.*?```", "");


            return classifyWithAI(title, shortBody);

        } catch (GithubApiException e) {
            log.warn("GitHub API 호출 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("AI 분류 실패 {}", e.getMessage());
            throw new ClassificationException(ErrorStatus.AI_API_FAIL);
        }
    }

    public Tag dummyClassify(String title, String body) {
        if (title.toLowerCase().contains("fix") || body.length() > 300) {
            return Tag.HARD;
        } else {
            return Tag.EASY;
        }
    }

    public Tag classifyWithAI(String title, String body) {
        try {
            String result = aiClassifierClient.classify(title, body).toLowerCase();
            return switch (result) {
                case "easy" -> Tag.EASY;
                case "medium" -> Tag.MEDIUM;
                case "hard" -> Tag.HARD;
                default -> Tag.MISC;
            };
        } catch (Exception e) {
            log.error("AI API 분류 실패", e);
            throw new ClassificationException(ErrorStatus.AI_API_FAIL);
        }
    }
}