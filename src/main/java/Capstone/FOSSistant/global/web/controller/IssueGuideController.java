package Capstone.FOSSistant.global.web.controller;

import Capstone.FOSSistant.global.apiPayload.ApiResponse;
import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
import Capstone.FOSSistant.global.service.GeminiGuideService;
import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
@Tag(name = "이슈 가이드 생성 API")
public class IssueGuideController {

    private final GeminiGuideService geminiGuideService;

    @PostMapping("/guide")
    @Operation(summary = "이슈 가이드 생성", description = "이슈 정보로부터 레포지토리 분석 및 가이드 생성")
    public ApiResponse<IssueGuideResponseDTO> getGuide(@RequestBody IssueGuideRequestDTO request) {
        return ApiResponse.onSuccess(SuccessStatus.ISSUE_GUIDE_OK,
                geminiGuideService.generateGuide(request.getIssueId(), request.getTitle(), request.getBody()));
    }
}
