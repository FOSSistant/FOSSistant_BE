package Capstone.FOSSistant.global.service;

import Capstone.FOSSistant.global.converter.IssueListConverter;
import Capstone.FOSSistant.global.domain.enums.Tag;
import Capstone.FOSSistant.global.repository.IssueListRepository;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional
public class IssueListServiceImpl implements IssueListService {

    private final IssueListRepository issueListRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public CompletableFuture<IssueListResponseDTO.IssueResponseDTO> classify(IssueListRequestDTO.IssueRequestDTO dto) {
        String redisKey = "issue:" + dto.getId();
        String cached = redisTemplate.opsForValue().get(redisKey);

        Tag difficulty;

        if (cached != null) {
            difficulty = Tag.valueOf(cached.toUpperCase());
        } else {
            // 실제 AI 로직 호출 대신 더미 처리
            difficulty = dummyClassify(dto);  // TODO: AI 연동 시 교체
            redisTemplate.opsForValue().set(redisKey, difficulty.name().toLowerCase(), Duration.ofDays(7));
            issueListRepository.save(IssueListConverter.toEntity(dto, difficulty));
        }

        return CompletableFuture.completedFuture(IssueListConverter.toResponseDTO(dto.getId(), difficulty));
    }

    private Tag dummyClassify(IssueListRequestDTO.IssueRequestDTO dto) {
        if (dto.getTitle().toLowerCase().contains("fix") || dto.getBody().length() > 300) {
            return Tag.HARD;
        } else {
            return Tag.EASY;
        }
    }
}