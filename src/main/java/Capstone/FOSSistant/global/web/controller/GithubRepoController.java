package Capstone.FOSSistant.global.web.controller;

import Capstone.FOSSistant.global.apiPayload.APiResponse;
import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.security.handler.annotation.AuthUser;
import Capstone.FOSSistant.global.service.githubRepo.GithubRepositoryService;
import Capstone.FOSSistant.global.service.githubRepo.GithubRepositoryServiceImpl;
import Capstone.FOSSistant.global.web.dto.GithubRepo.GithubRepoDTO;
import Capstone.FOSSistant.global.web.dto.util.custom.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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

//    @PostMapping("/trending")
//    @Operation(summary = "트렌딩 레포지터리 저장", description = "언어별 트렌딩 레포지터리 15개씩 가져와 DB에 저장")
//    @ApiErrorCodeExamples({ErrorStatus.GITHUB_API_FAIL, ErrorStatus._INTERNAL_SERVER_ERROR})
//    public APiResponse<Void> storeTrendingRepositories() {
//        log.info("트렌딩 레포지터리 저장 요청 수신");
//        githubRepositoryService.fetchAndStoreTrendingRepositories();
//        return APiResponse.onSuccess(SuccessStatus.REPO_CRAWLING_OK, null);
//    }

    @GetMapping("/personal")
    @Operation(
            summary = "사용자 Top 3 언어 기반 레포 추천",
            description = "로그인한 사용자의 Top 3 언어별로 GitHub 트렌딩 레포지터리 중 무작위로 1개씩 추천 (총 3개)"
    )
    public APiResponse<GithubRepoDTO.GithubRepoListDTO> recommendUserTopLangRepos(@AuthUser @Parameter(hidden = true) Member member) {
        return APiResponse.onSuccess(SuccessStatus.REPO_RECOMMEND_OK,
                githubRepositoryService.recommendTopRepositories(member));
    }

    @GetMapping("/category/{language}")
    @Operation(
            summary = "언어별 레포지터리 추천",
            description = "지정한 언어(Java, TypeScript, JavaScript, Python, Jupyter Notebook)에 해당하는 무작위 레포지터리 3개를 반환합니다."
    )
    public APiResponse<GithubRepoDTO.GithubRepoListDTO> recommendByLanguage(
            @Parameter(
                    description = "프론트에서 선택한 언어. 다음 중 하나여야 합니다: Java, TypeScript, JavaScript, Python, Jupyter Notebook",
                    schema = @Schema(allowableValues = {
                            "Java", "TypeScript", "JavaScript", "Python", "Jupyter Notebook"
                    })
            )
            @PathVariable String language
    ) {
        return APiResponse.onSuccess(SuccessStatus.REPO_RECOMMEND_OK,
                githubRepositoryService.recommendByLanguage(language));
    }

}
