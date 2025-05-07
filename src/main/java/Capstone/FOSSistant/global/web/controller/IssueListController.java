package Capstone.FOSSistant.global.web.controller;

import Capstone.FOSSistant.global.apiPayload.ApiResponse;
import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
import Capstone.FOSSistant.global.service.IssueListServiceImpl;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/api/issues")
@Tag(name = "이슈 분류 API", description = "GitHub 이슈를 AI로 난이도 분류하는 API입니다.")
public class IssueListController {

    private final IssueListServiceImpl issueListService;

    @PostMapping
    @Operation(summary = "이슈 AI 분류", description = "여러 개의 GitHub 이슈에 대해 AI 기반 난이도 분류 수행")
    public ApiResponse<IssueListResponseDTO> classifyIssues(
            @RequestBody IssueListRequestDTO requestDTO) {

        List<CompletableFuture<IssueListResponseDTO.IssueResponseDTO>> futures = requestDTO.getIssues().stream()
                .map(issueListService::classify)
                .toList();

        List<IssueListResponseDTO.IssueResponseDTO> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        return ApiResponse.onSuccess(SuccessStatus.ISSUE_OK,
                IssueListResponseDTO.builder().results(results).build());
    }
}