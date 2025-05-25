package Capstone.FOSSistant.global.web.controller;

import Capstone.FOSSistant.global.apiPayload.APiResponse;
import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.security.handler.annotation.AuthUser;
import Capstone.FOSSistant.global.service.member.MemberService;
import Capstone.FOSSistant.global.web.dto.Member.AuthRequestDTO;
import io.lettuce.core.dynamic.annotation.Param;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
            @RequestBody AuthRequestDTO.UpdateLevelRequest request
    ) {
        memberService.updateLevel(member, request.getLevel());
        return APiResponse.onSuccess(SuccessStatus.MEMBER_LEVEL_OK, null);
    }
}