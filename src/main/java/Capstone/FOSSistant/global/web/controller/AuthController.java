package Capstone.FOSSistant.global.web.controller;

import Capstone.FOSSistant.global.apiPayload.APiResponse;
import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.security.handler.annotation.AuthUser;
import Capstone.FOSSistant.global.service.member.MemberService;
import Capstone.FOSSistant.global.web.dto.Member.AuthRequestDTO;
import Capstone.FOSSistant.global.web.dto.Member.AuthResponseDTO;
import Capstone.FOSSistant.global.web.dto.util.custom.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/auth")
@Tag(name = "GitHub 로그인/회원가입/로그아웃 API")
public class AuthController {
    private final MemberService memberService;

    @PostMapping("/github")
    @ApiErrorCodeExamples({
            ErrorStatus.AUTH_GITHUB_FAIL,    // 깃허브 OAuth 교환 실패
            ErrorStatus.AUTH_INVALID_TOKEN,   // 잘못된 또는 만료된 코드
            ErrorStatus._INTERNAL_SERVER_ERROR
    })
    @Operation(
            summary = "GitHub 로그인 / 회원가입",
            description = "GitHub OAuth code를 받아 로그인 또는 회원가입을 수행합니다."
    )
    public APiResponse<AuthResponseDTO.OAuthResponse> loginWithGitHub(
            @RequestBody @Valid AuthRequestDTO.GithubRequest request) {
        var tokens = memberService.loginOrSignUp(request.getGithubCode());
        return APiResponse.onSuccess(SuccessStatus.MEMBER_LOGIN_OK, tokens);
    }

    @PostMapping("/token/refresh")
    @ApiErrorCodeExamples({
            ErrorStatus.AUTH_INVALID_TOKEN,  // 리프레시 토큰 불일치/만료
            ErrorStatus.NOT_CONTAIN_TOKEN,   // Redis에 토큰이 없음
            ErrorStatus._INTERNAL_SERVER_ERROR
    })
    @Operation(
            summary = "AccessToken 재발급",
            description = "RefreshToken으로 AccessToken을 재발급합니다."
    )
    public APiResponse<AuthResponseDTO.TokenRefreshResponse> refreshToken(
            @RequestBody @Valid AuthRequestDTO.RefreshToken request) {

        String accessToken = memberService.reissueAccessToken(request.getRefreshToken());
        return APiResponse.onSuccess(
                SuccessStatus.ACCESSTOKEN_OK,
                AuthResponseDTO.TokenRefreshResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(request.getRefreshToken())
                        .build()
        );
    }

    @PostMapping("/logout")
    @ApiErrorCodeExamples({
            ErrorStatus.AUTH_INVALID_TOKEN,  // 토큰이 유효하지 않음
            ErrorStatus.NOT_CONTAIN_TOKEN,   // 이미 로그아웃된 상태
            ErrorStatus._INTERNAL_SERVER_ERROR
    })
    @Operation(
            summary = "로그아웃",
            description = "RefreshToken을 삭제해 로그아웃합니다."
    )
    public APiResponse<Void> logout(
            @AuthUser @Parameter(hidden = true) Member member) {

        memberService.logout(member);
        return APiResponse.onSuccess(SuccessStatus.MEMBER_LOGOUT_OK, null);
    }

    @DeleteMapping("/withdraw")
    @ApiErrorCodeExamples({
            ErrorStatus.AUTH_INVALID_TOKEN,  // 토큰이 유효하지 않음
            ErrorStatus.MEMBER_NOT_FOUND,    // 이미 탈퇴했거나 존재하지 않음
            ErrorStatus._INTERNAL_SERVER_ERROR
    })
    @Operation(
            summary = "회원 탈퇴",
            description = "RefreshToken을 삭제하고 DB에서 회원을 제거합니다."
    )
    public APiResponse<Void> withdraw(
            @AuthUser @Parameter(hidden = true) Member member) {

        memberService.withdraw(member);
        return APiResponse.onSuccess(SuccessStatus.MEMBER_WITHDRAW_OK, null);
    }

    @PostMapping("/test/token")

    @Operation(
            summary = "서버에서 테스트 하기 위한 API",
            description = "memberId입력으로 AccessToken 발급."
    )
    public APiResponse<AuthResponseDTO.OAuthResponse> issueTestTokens(
            @RequestBody @Valid AuthRequestDTO.DevTokenRequest request
    ) {
        var tokens = memberService.getServerAccessToken(request.getMemberId());
        return APiResponse.onSuccess(SuccessStatus.MEMBER_LOGIN_OK, tokens);
    }
}