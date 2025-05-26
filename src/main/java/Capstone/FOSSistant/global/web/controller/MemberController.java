package Capstone.FOSSistant.global.web.controller;

import Capstone.FOSSistant.global.apiPayload.APiResponse;
import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.security.handler.annotation.AuthUser;
import Capstone.FOSSistant.global.service.member.MemberService;
import Capstone.FOSSistant.global.web.dto.Member.AuthRequestDTO;
import Capstone.FOSSistant.global.web.dto.Member.MemberRequestDTO;
import Capstone.FOSSistant.global.web.dto.Member.MemberResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/member")
@Tag(name = "회원 관련 API", description = "회원정보를 설정하는 API입니다.")
public class MemberController {

    private final MemberService memberService;

    @PatchMapping("/level")
    @Operation(summary = "레벨 설정 API", description = "오픈소스 기여 여부를 입력하는 API")
    public APiResponse<Void> updateLevel(
            @AuthUser @Parameter(hidden = true) Member member,
            @Valid @RequestBody MemberRequestDTO.UpdateLevelRequest request
    ) {
        memberService.updateLevel(member, request.getLevel());
        return APiResponse.onSuccess(SuccessStatus.MEMBER_LEVEL_OK, null);
    }

    @GetMapping("/profile")
    @Operation(summary = "회원 프로필 조회", description = "로그인된 회원의 GitHub 닉네임, 프로필 이미지, 레벨을 반환합니다.")
    public APiResponse<MemberResponseDTO.MemberProfileResponseDTO> getProfile(
            @AuthUser @Parameter(hidden = true) Member member
    ) {
        var dto = memberService.getProfile(member);
        return APiResponse.onSuccess(SuccessStatus.MEMBER_PROFILE_OK, dto);
    }

}