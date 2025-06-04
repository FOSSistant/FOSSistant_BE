package Capstone.FOSSistant.global.web.controller;

import Capstone.FOSSistant.global.apiPayload.APiResponse;
import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
import Capstone.FOSSistant.global.service.githubRepo.GithubRepositoryServiceImpl;
import Capstone.FOSSistant.global.web.dto.util.custom.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/repo")
@Tag(name = "GitHub 트렌딩 저장 API", description = "언어별 GitHub 트렌딩 레포지터리를 15개씩 DB에 저장")
public class GithubRepoController {

    private final GithubRepositoryServiceImpl githubRepositoryService;

    @PostMapping("/trending")
    @Operation(summary = "트렌딩 레포지터리 저장", description = "언어별 트렌딩 레포지터리 15개씩 가져와 DB에 저장")
    @ApiErrorCodeExamples({ErrorStatus.GITHUB_API_FAIL, ErrorStatus._INTERNAL_SERVER_ERROR})
    public APiResponse<Void> storeTrendingRepositories() {
        log.info("트렌딩 레포지터리 저장 요청 수신");
        githubRepositoryService.fetchAndStoreTrendingRepositories();
        return APiResponse.onSuccess(SuccessStatus.REPO_CRAWLING_OK, null);
    }
}
