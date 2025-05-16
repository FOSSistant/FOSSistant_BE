package Capstone.FOSSistant.global.web.controller;


import Capstone.FOSSistant.global.apiPayload.APiResponse;
import Capstone.FOSSistant.global.apiPayload.code.ErrorReasonDTO;
import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
import Capstone.FOSSistant.global.service.IssueListServiceImpl;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListResponseDTO;
import Capstone.FOSSistant.global.web.dto.util.custom.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jdk.jshell.spi.ExecutionControl;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/issues")
@Tag(name = "이슈 분류 API", description = "GitHub 이슈를 AI로 난이도 분류하는 API입니다.")
public class IssueListController {

    private final IssueListServiceImpl issueListService;

    @PostMapping
    @Operation(summary = "이슈 AI 분류", description = "여러 개의 GitHub 이슈에 대해 AI 기반 난이도 분류 수행")
    @ApiErrorCodeExamples({ErrorStatus.AI_API_FAIL, ErrorStatus.GITHUB_API_FAIL, ErrorStatus._INTERNAL_SERVER_ERROR, ErrorStatus.REDIS_CONNECTION_FAIL})
    public APiResponse<IssueListResponseDTO> classifyIssues(
            @RequestBody @Valid IssueListRequestDTO requestDTO) {
        // 서비스에서 batch, 병렬, sleep 처리
        List<CompletableFuture<IssueListResponseDTO.IssueResponseDTO>> futures =
                issueListService.classifyAll(requestDTO.getIssues());

        // 모든 Future 결과 모음
        List<IssueListResponseDTO.IssueResponseDTO> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        return APiResponse.onSuccess(SuccessStatus.ISSUE_TAGGING_OK,
                IssueListResponseDTO.builder().results(results).build());
    }
}