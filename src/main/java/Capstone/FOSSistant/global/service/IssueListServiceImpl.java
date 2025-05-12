package Capstone.FOSSistant.global.service;

import Capstone.FOSSistant.global.config.GitHubHelper;
import Capstone.FOSSistant.global.converter.IssueListConverter;
import Capstone.FOSSistant.global.domain.enums.Tag;
import Capstone.FOSSistant.global.repository.IssueListRepository;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final GitHubHelper githubHelper;

    @Override
    public CompletableFuture<IssueListResponseDTO.IssueResponseDTO> classify(IssueListRequestDTO.IssueRequestDTO dto) {
        String redisKey = "issue:" + dto.getIssueId();
        Tag difficulty;

        try {
            String cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                difficulty = Tag.valueOf(cached.toUpperCase());
                log.info("캐시 조회 성공: {}", redisKey);
            } else {
                difficulty = safeClassify(dto.getIssueId());
                try {
                    redisTemplate.opsForValue().set(redisKey, difficulty.name().toLowerCase(), Duration.ofDays(7));
                    log.info("캐시 저장 완료: {}", redisKey);
                } catch (Exception e) {
                    log.warn("캐시 저장 실패: {}", e.getMessage());
                }

                try {
                    issueListRepository.save(IssueListConverter.toEntity(dto, difficulty));
                } catch (Exception e) {
                    log.warn("DB 저장 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("예외 발생, fallback 처리: {}", e.getMessage());
            difficulty = Tag.UNKNOWN;
        }

        return CompletableFuture.completedFuture(
                IssueListConverter.toResponseDTO(dto.getIssueId(), difficulty)
        );
    }

    private Tag safeClassify(String issueUrl) {
        try {
            // 이슈 URL → owner, repo, issueNumber 추출
            String[] parts = issueUrl.split("/");
            String owner = parts[3];
            String repo = parts[4];
            String issueNumber = parts[6];

            // GitHub API로 title/body 가져오기
            String title = githubHelper.fetchIssueTitle(owner, repo, issueNumber);
            String body = githubHelper.fetchIssueBody(owner, repo, issueNumber);

            return dummyClassify(title, body);

        } catch (Exception e) {
            log.warn("분류 실패: {}", e.getMessage());
            return Tag.UNKNOWN;
        }
    }

    public Tag dummyClassify(String title, String body) {
        if (title.toLowerCase().contains("fix") || body.length() > 300) {
            return Tag.HARD;
        } else {
            return Tag.EASY;
        }
    }
}