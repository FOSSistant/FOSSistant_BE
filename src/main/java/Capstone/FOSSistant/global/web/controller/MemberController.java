package Capstone.FOSSistant.global.web.controller;

import Capstone.FOSSistant.global.apiPayload.APiResponse;
import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.security.handler.annotation.AuthUser;
import Capstone.FOSSistant.global.service.member.MemberService;
import Capstone.FOSSistant.global.web.dto.Member.AuthRequestDTO;
import Capstone.FOSSistant.global.web.dto.Member.AuthResponseDTO;
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
@RequestMapping("/auth")
@Tag(name = "Github 로그인/회원가입/로그아웃 API")
public class MemberController {
    private final MemberService memberService;
    @PostMapping("/github")
    @Operation(summary = "GitHub 로그인 / 회원가입", description = "GitHub OAuth code를 받아 로그인 또는 회원가입을 수행합니다.")
    public APiResponse<AuthResponseDTO.OAuthResponse> loginWithGitHub(
            @RequestBody @Valid AuthRequestDTO.GithubRequest request) {

        AuthResponseDTO.OAuthResponse tokens = memberService.loginOrSignUp(request.getGithubCode());
        return APiResponse.onSuccess(SuccessStatus.MEMBER_LOGIN_OK, tokens);
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "AccessToken을 재발급합니다", description = "RefreshToken으로 AccessToken을 재발급합니다.")
    public APiResponse<AuthResponseDTO.TokenRefreshResponse> refreshToken(
            @RequestBody @Valid AuthRequestDTO.RefreshToken request) {

        String refreshToken = request.getRefreshToken();
        String accessToken = memberService.reissueAccessToken(refreshToken);
        return APiResponse.onSuccess(SuccessStatus.ACCESSTOKEN_OK,
                AuthResponseDTO.TokenRefreshResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "RefreshToken을 삭제해 로그아웃합니다.")
    public APiResponse<Void> logout(@AuthUser @Parameter(hidden = true) Member member) {
        memberService.logout(member);
        return APiResponse.onSuccess(SuccessStatus.MEMBER_LOGOUT_OK, null);
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "탈퇴", description = "RefreshToken, DB에 member를 삭제해 탈퇴합니다.")
    public APiResponse<Void> withdraw(@AuthUser @Parameter(hidden = true) Member member) {
        memberService.withdraw(member);
        return APiResponse.onSuccess(SuccessStatus.MEMBER_WITHDRAW_OK, null);
    }
}
