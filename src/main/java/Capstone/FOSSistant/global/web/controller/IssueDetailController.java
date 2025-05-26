//package Capstone.FOSSistant.global.web.controller;
//
//import Capstone.FOSSistant.global.apiPayload.APiResponse;
//import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
//import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
//import Capstone.FOSSistant.global.service.llm.GeminiGuideService;
//import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideRequestDTO;
//import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideResponseDTO;
//import Capstone.FOSSistant.global.web.dto.util.custom.ApiErrorCodeExamples;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@Validated
//@RequestMapping("/issues")
//@RequiredArgsConstructor
//@Tag(name = "이슈 가이드 생성 API")
//public class IssueGuideController {
//
//    private final GeminiGuideService geminiGuideService;
//
//    @PostMapping("/guide")
//    @ApiErrorCodeExamples({ErrorStatus.AI_API_FAIL, ErrorStatus.GITHUB_API_FAIL, ErrorStatus._INTERNAL_SERVER_ERROR})
//    @Operation(summary = "이슈 가이드 생성", description = "이슈 URL로부터 title/body/readme 구조를 추출하여 가이드 생성")
//    public APiResponse<IssueGuideResponseDTO> getGuide(@RequestBody @Valid IssueGuideRequestDTO request) {
//        return APiResponse.onSuccess(
//                SuccessStatus.ISSUE_GUIDE_OK,
//                geminiGuideService.generateGuide(request.getIssueId())
//        );
//    }
//}
package Capstone.FOSSistant.global.web.controller;

import Capstone.FOSSistant.global.apiPayload.APiResponse;
import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.security.handler.annotation.AuthUser;
import Capstone.FOSSistant.global.service.llm.IssueDetailService;
import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueFeedbackRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueFeedbackResponseDTO;
import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueGuide.IssueGuideResponseDTO;
import Capstone.FOSSistant.global.web.dto.util.custom.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@Validated
@RequestMapping("/issues")
@RequiredArgsConstructor
@Tag(name = "상세 이슈 API")
public class IssueDetailController {

    private final IssueDetailService issueDetailService;

    @PostMapping("/guide")
    @ApiErrorCodeExamples({ErrorStatus.AI_API_FAIL, ErrorStatus.GITHUB_API_FAIL, ErrorStatus._INTERNAL_SERVER_ERROR})
    @Operation(summary = "이슈 가이드 생성", description = "이슈 URL로부터 title/body/readme 구조를 추출하여 가이드 생성")
    public CompletableFuture<APiResponse<IssueGuideResponseDTO>> getGuide(
            @AuthUser @Parameter(hidden = true) Member member,
            @RequestBody @Valid IssueGuideRequestDTO request) {
        return issueDetailService.generateGuide(request.getIssueId())
                .thenApply(guide -> APiResponse.onSuccess(SuccessStatus.ISSUE_GUIDE_OK, guide));
    }

    @PostMapping("/feedback")
    @Operation(summary = "이슈 피드백 등록/수정",
            description = "issueId, feedbackTag를 바디로 받아 생성 또는 업데이트합니다.")
    public APiResponse<Void> upsertFeedback(
            @AuthUser @Parameter(hidden = true) Member member,
            @Valid @RequestBody IssueFeedbackRequestDTO.Feedback request
    ) {
        issueDetailService.upsertFeedback(
                member,
                request.getIssueId(),
                request.getFeedbackTag()
        );
        return APiResponse.onSuccess(SuccessStatus.ISSUE_FEEDBACK_OK, null);
    }

    @GetMapping("/feedback")
    @Operation(summary = "내가 남긴 피드백 조회", description = "issueId에 대해 내가 남긴 난이도 피드백을 반환")
    public APiResponse<IssueFeedbackResponseDTO.Feedback> getMyFeedback(
            @AuthUser @Parameter(hidden = true) Member member,
            @RequestParam String issueId
    ) {
        Capstone.FOSSistant.global.domain.enums.Tag tag = issueDetailService.getMyFeedback(member, issueId);
        if (tag == null) {
            return APiResponse.onSuccess(SuccessStatus.ISSUE_FEEDBACK_FOUND, null);
        }
        return APiResponse.onSuccess(
                SuccessStatus.ISSUE_FEEDBACK_FOUND,
                IssueFeedbackResponseDTO.Feedback.builder()
                        .issueId(issueId)
                        .feedbackTag(tag)
                        .build()
        );
    }
}