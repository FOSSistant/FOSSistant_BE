package Capstone.FOSSistant.global.service;

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

    @Override
    public CompletableFuture<IssueListResponseDTO.IssueResponseDTO> classify(IssueListRequestDTO.IssueRequestDTO dto) {
        String redisKey = "issue:" + dto.getId();
        Tag difficulty;

        try {
            String cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                difficulty = Tag.valueOf(cached.toUpperCase());
                log.info("캐시 조회 성공: {}", redisKey);
            } else {
                difficulty = safeClassify(dto);
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
                IssueListConverter.toResponseDTO(dto.getId(), difficulty)
        );
    }

    private Tag safeClassify(IssueListRequestDTO.IssueRequestDTO dto) {
        try {
            return dummyClassify(dto); // 실제 AI 로직으로 대체 예정
        } catch (Exception e) {
            log.warn("분류 실패: {}", e.getMessage());
            return Tag.UNKNOWN;
        }
    }

    private Tag dummyClassify(IssueListRequestDTO.IssueRequestDTO dto) {
        if (dto.getTitle().toLowerCase().contains("fix") || dto.getBody().length() > 300) {
            return Tag.HARD;
        } else {
            return Tag.EASY;
        }
    }
}