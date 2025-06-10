package Capstone.FOSSistant.global.service.customAI;

import Capstone.FOSSistant.global.aop.annotation.MeasureExecutionTime;
import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.exception.ClassificationException;
import Capstone.FOSSistant.global.converter.IssueListConverter;
import Capstone.FOSSistant.global.domain.entity.IssueList;
import Capstone.FOSSistant.global.domain.enums.Tag;
import Capstone.FOSSistant.global.repository.IssueListRepository;
import Capstone.FOSSistant.global.service.GitHubHelperService;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IssueListServiceImpl implements IssueListService {
    private static final double SCORE_THRESHOLD = 0.4;

    private final IssueListRepository issueListRepository;
    private final StringRedisTemplate redisTemplate;
    private final GitHubHelperService githubHelperService;
    private final IssueListConverter issueListConverter;
    private final AIClassifierClient aiClassifierClient;
    private final ThreadPoolTaskExecutor classifierExecutor; // 커스텀 풀

    /**
     * 여러 이슈를 batchSize=5씩 묶어 병렬로 AI 분류 요청을 보냅니다.
     */
    @Override
    @MeasureExecutionTime
    public List<CompletableFuture<IssueListResponseDTO.IssueResponseDTO>> classifyAll(
            List<IssueListRequestDTO.IssueRequestDTO> dtoList) {

        int batchSize = 5;
        List<CompletableFuture<IssueListResponseDTO.IssueResponseDTO>> allFutures = new ArrayList<>();

        for (int i = 0; i < dtoList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dtoList.size());
            List<IssueListRequestDTO.IssueRequestDTO> batch = dtoList.subList(i, end);
            allFutures.addAll(classifyBatch(batch));
            // Thread.sleep 제거 → 풀 크기와 큐 용량으로 부하를 제어합니다.
        }

        return allFutures;
    }

    /**
     * batch(최대 5개) 단위로 분류 요청을 처리합니다.
     * 캐시된 항목은 즉시 CompletableFuture.completedFuture로, 미캐시된 항목만 AI 서버 호출 및 DB 저장을 수행
     */
    private List<CompletableFuture<IssueListResponseDTO.IssueResponseDTO>> classifyBatch(
            List<IssueListRequestDTO.IssueRequestDTO> batch) {

        List<CompletableFuture<IssueListResponseDTO.IssueResponseDTO>> futures = new ArrayList<>();
        List<IssueListRequestDTO.IssueRequestDTO> toClassify = new ArrayList<>();
        List<Integer> indexesToClassify = new ArrayList<>();

        // 1) Redis 캐시 및 DB 검사
        for (int i = 0; i < batch.size(); i++) {
            IssueListRequestDTO.IssueRequestDTO dto = batch.get(i);
            String key = "issue:" + dto.getIssueId();
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                Tag tag = Tag.valueOf(cached.toUpperCase());
                // 즉시 completedFuture 리턴
                futures.add(
                        CompletableFuture.completedFuture(
                                issueListConverter.toResponseDTO(dto.getIssueId(), tag)
                        )
                );
                continue;
            }
            // DB 체크
            IssueList existingIssue = issueListRepository.findById(dto.getIssueId()).orElse(null);
            if (existingIssue != null) {
                futures.add(CompletableFuture.completedFuture(
                        issueListConverter.toResponseDTO(dto.getIssueId(), existingIssue.getDifficulty())));
                continue;
            }
            toClassify.add(dto);
            indexesToClassify.add(i);
        }

        // 2) 미캐시된 항목만 AI 서버에 batch 처리
        if (!toClassify.isEmpty()) {
            // (A) GitHub API → AI → 파싱 → 캐시/DB 저장 → DTO 반환 과정을 합친 CompletableFuture 리스트 생성
            List<CompletableFuture<IssueClassificationResult>> classificationFutures = new ArrayList<>();

            for (IssueListRequestDTO.IssueRequestDTO dto : toClassify) {
                // GitHub API 호출 (제목/본문)
                CompletableFuture<String[]> gitHubFuture = CompletableFuture.supplyAsync(
                        () -> extractTitleAndBody(dto.getIssueId()),
                        classifierExecutor
                );

                // GitHub 결과를 받은 뒤 AI 분류
                CompletableFuture<Tag> aiFuture = gitHubFuture.thenComposeAsync(parts -> {
                    String title = parts[0];
                    String body = parts[1];
                    return classifyWithAI(title, body);
                }, classifierExecutor);

                // AI 분류 결과를 받은 뒤 캐시 저장/DB 저장 + DTO 변환
                CompletableFuture<IssueClassificationResult> resultFuture = aiFuture.thenApplyAsync(tag -> {
                    // 캐시 저장
                    String key = "issue:" + dto.getIssueId();
                    cacheDifficulty(key, tag);
                    // DB 저장
                    persistDifficulty(dto, tag);
                    // 응답 DTO 생성
                    IssueListResponseDTO.IssueResponseDTO responseDTO =
                            issueListConverter.toResponseDTO(dto.getIssueId(), tag);

                    return new IssueClassificationResult(dto.getIssueId(), responseDTO);
                }, classifierExecutor);

                classificationFutures.add(resultFuture);
            }

            // (B) 모든 classificationFutures가 완료될 때까지 대기
            CompletableFuture<Void> allDone = CompletableFuture
                    .allOf(classificationFutures.toArray(new CompletableFuture[0]));

            // (C) 결과 리스트로 변환
            CompletableFuture<List<IssueClassificationResult>> allResults = allDone.thenApply(v ->
                    classificationFutures.stream()
                            .map(CompletableFuture::join)
                            .toList()
            );

            // (D) allResults에서 결과를 가져와, futures 리스트의 원래 순서에 맞게 삽입
            for (int idx = 0; idx < toClassify.size(); idx++) {
                int batchIndex = indexesToClassify.get(idx);
                final int finalIdx = idx; // 람다 내에서 사용하려면 효과적 final이어야 합니다.

                CompletableFuture<IssueListResponseDTO.IssueResponseDTO> wrapperFuture =
                        allResults.thenApplyAsync(list -> {
                            // list 안에서 issueId가 같을 때의 DTO를 찾아 리턴
                            for (IssueClassificationResult r : list) {
                                if (r.issueId.equals(toClassify.get(finalIdx).getIssueId())) {
                                    return r.responseDTO;
                                }
                            }
                            // 실패 시 기본값 반환
                            return issueListConverter.toResponseDTO(toClassify.get(finalIdx).getIssueId(), Tag.MISC);
                        }, classifierExecutor);

                futures.add(batchIndex, wrapperFuture);
            }
        }

        return futures;
    }

    /**
     * GitHub API 호출로 이슈의 title, body를 가져옵니다.
     */
    @MeasureExecutionTime
    private String[] extractTitleAndBody(String issueUrl) {
        try {
            String[] parts = issueUrl.split("/");
            String owner = parts[3];
            String repo = parts[4];
            String issueNumber = parts[6];
            String[] titleAndBody = githubHelperService.fetchIssueData(owner, repo, issueNumber);
            return new String[]{ titleAndBody[0], titleAndBody[1] };
        } catch (Exception e) {
            throw new ClassificationException(ErrorStatus.AI_API_FAIL);
        }
    }

    /**
     * AI 서버에 단건 분류 요청을 보내고, JSON 문자열을 Tag로 파싱하여 CompletableFuture<Tag>로 반환합니다.
     * 내부적으로 WebClient Mono → toFuture()로 변환되며, 파싱 단계는 classifierExecutor에서 실행됩니다.
     */
    public CompletableFuture<Tag> classifyWithAI(String title, String body) {
        long start = System.currentTimeMillis();

        return aiClassifierClient.classify(title, body)
                .timeout(Duration.ofSeconds(20))
                // 1) 네트워크/타임아웃 에러
                .onErrorResume(TimeoutException.class, e -> {
                    log.error("[{}] AI 호출 타임아웃 ({}ms) — issue: {}",
                            "NETWORK", System.currentTimeMillis() - start, title, e);
                    // fallback JSON
                    return aiClassifierClient.defaultSingleResult();
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("[{}] AI HTTP 에러: {} {} — issue: {}",
                            "HTTP", e.getRawStatusCode(), e.getStatusText(), title, e);
                    return aiClassifierClient.defaultSingleResult();
                })
                .onErrorResume(e -> {
                    log.error("[{}] AI 호출 예외: {} — issue: {}",
                            "UNKNOWN_CALL", e.getMessage(), title, e);
                    return aiClassifierClient.defaultSingleResult();
                })
                .toFuture()
                .thenApplyAsync(jsonResult -> {
                    try {
                        // 2) JSON 파싱
                        Tag tag = parseJsonToTag(jsonResult);
                        log.info("[{}] 분류 완료: {} ({}ms) — issue: {}",
                                "SUCCESS", tag, System.currentTimeMillis() - start, title);
                        return tag;
                    } catch (Exception parseEx) {
                        // 3) 파싱 오류
                        log.error("[{}] AI 응답 파싱 실패 ({}ms) — issue: {}, json={}",
                                "PARSE_ERROR", System.currentTimeMillis() - start, title, jsonResult, parseEx);
                        return Tag.UNKNOWN;
                    }
                }, classifierExecutor);
    }

    /**
     * AI 서버의 배치 분류를 요청하고, JSON 결과를 List<Tag>로 파싱하여 CompletableFuture<List<Tag>>로 반환합니다.
     */
    private CompletableFuture<List<Tag>> classifyWithAIBatch(List<String[]> titleBodyList) {
        // (1) WebClient 논블로킹 배치 호출 → Mono<String> → toFuture() → CompletableFuture<String>
        CompletableFuture<String> monoFuture = aiClassifierClient.classifyBatch(titleBodyList)
                .timeout(Duration.ofSeconds(30))
                // 에러 시 기본 JSON 반환
                .onErrorResume(e -> aiClassifierClient.defaultBatchResult())
                .toFuture();

        // (2) JSON 파싱 → List<Tag> 반환
        return monoFuture.thenApplyAsync(result -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode root = objectMapper.readTree(result).get("results");
                List<Tag> tags = new ArrayList<>();

                if (root != null && root.isArray()) {
                    for (JsonNode node : root) {
                        String diff = node.get("difficulty").asText("misc").toLowerCase();
                        tags.add(switch (diff) {
                            case "easy" -> Tag.EASY;
                            case "medium" -> Tag.MEDIUM;
                            case "hard" -> Tag.HARD;
                            default -> Tag.MISC;
                        });
                    }
                }
                // 만약 results가 null이거나 빈 배열이라면
                if (tags.isEmpty()) {
                    // 기본값으로 모두 MISC
                    return titleBodyList.stream().map(v -> Tag.MISC).toList();
                }
                return tags;
            } catch (Exception e) {
                log.error("AI batch JSON 파싱 실패 - result: {}", result, e);
                return titleBodyList.stream().map(v -> Tag.MISC).toList();
            }
        }, classifierExecutor);
    }


    @MeasureExecutionTime
    private void cacheDifficulty(String key, Tag difficulty) {
        try {
            redisTemplate.opsForValue().set(key, difficulty.name().toLowerCase(), Duration.ofDays(7));
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 저장 실패: {}", e.getMessage());
        }
    }


    @MeasureExecutionTime
    private void persistDifficulty(IssueListRequestDTO.IssueRequestDTO dto, Tag difficulty) {
        try {
            var entity = issueListConverter.toEntity(dto, difficulty);
            issueListRepository.save(entity);
        } catch (DataAccessException e) {
            log.warn("DB 저장 실패: {}", e.getMessage());
        }
    }


    private Tag parseJsonToTag(String json) {
        try {
            JsonNode results = new ObjectMapper()
                    .readTree(json)
                    .get("results");

            if (results != null && results.isArray() && results.size() > 0) {
                JsonNode first = results.get(0);
                String diff = first.get("difficulty").asText("misc").toLowerCase();
                double score = first.has("score")
                        ? first.get("score").asDouble()
                        : 1.0;  // score 필드가 없으면 기본 1.0

                // 1) misc는 모델이 예측하지 않기로 한 케이스
                if ("misc".equals(diff)) {
                    return Tag.MISC;
                }
                // 2) score가 임계치 미만이면 확신 부족으로 UNKNOWN
                if (score < SCORE_THRESHOLD) {
                    return Tag.UNKNOWN;
                }
                // 3) 그 외 easy/medium/hard 대로 반환
                return switch (diff) {
                    case "easy" -> Tag.EASY;
                    case "medium" -> Tag.MEDIUM;
                    case "hard" -> Tag.HARD;
                    default -> Tag.MISC;  // safety
                };
            }
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", e.getMessage(), e);
        }
        // 파싱 오류 시에도 UNKNOWN (예측은 했지만 올바른 결과가 아님)
        return Tag.UNKNOWN;
    }

    private static class IssueClassificationResult {
        final String issueId;
        final IssueListResponseDTO.IssueResponseDTO responseDTO;

        IssueClassificationResult(String issueId, IssueListResponseDTO.IssueResponseDTO responseDTO) {
            this.issueId = issueId;
            this.responseDTO = responseDTO;
        }
    }

    /**
     * 단건 분류 요청 (캐시/DB/AI 순)
     */
    public CompletableFuture<IssueListResponseDTO.IssueResponseDTO> classify(IssueListRequestDTO.IssueRequestDTO dto) {
        String key = "issue:" + dto.getIssueId();
        return CompletableFuture.supplyAsync(() -> {
            // DB 우선 체크
            IssueList existing = issueListRepository.findById(dto.getIssueId()).orElse(null);
            if (existing != null) {
                return "DB:" + existing.getDifficulty().name().toLowerCase();
            }
            String redisCached = redisTemplate.opsForValue().get(key);
            return redisCached != null ? "REDIS:" + redisCached : null;
        }, classifierExecutor).thenCompose(source -> {
            if (source != null) {
                if (source.startsWith("DB:")) {
                    Tag tag = Tag.valueOf(source.substring(3).toUpperCase());
                    return CompletableFuture.completedFuture(issueListConverter.toResponseDTO(dto.getIssueId(), tag));
                } else { // REDIS case
                    Tag tag = Tag.valueOf(source.substring(6).toUpperCase());
                    return CompletableFuture.completedFuture(issueListConverter.toResponseDTO(dto.getIssueId(), tag));
                }
            }
            // GitHub fetch + AI classify
            CompletableFuture<Tag> tagFuture = CompletableFuture.supplyAsync(
                    () -> extractTitleAndBody(dto.getIssueId()), classifierExecutor
            ).thenComposeAsync(parts -> classifyWithAI(parts[0], parts[1]), classifierExecutor);

            return tagFuture.thenApplyAsync(tag -> {
                cacheDifficulty(key, tag);
                persistDifficulty(dto, tag);
                return issueListConverter.toResponseDTO(dto.getIssueId(), tag);
            }, classifierExecutor);
        });
    }
}