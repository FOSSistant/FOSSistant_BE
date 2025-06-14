package Capstone.FOSSistant.global.service.member;

import Capstone.FOSSistant.global.apiPayload.APiResponse;
import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.code.status.SuccessStatus;
import Capstone.FOSSistant.global.apiPayload.exception.AuthException;
import Capstone.FOSSistant.global.apiPayload.exception.MemberException;
import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.domain.enums.Level;
import Capstone.FOSSistant.global.repository.MemberRepository;
import Capstone.FOSSistant.global.security.oauth.GitHubOAuthClient;
import Capstone.FOSSistant.global.security.provider.JwtTokenProvider;
import Capstone.FOSSistant.global.service.GitHubHelperService;
import Capstone.FOSSistant.global.web.dto.Member.AuthResponseDTO;
import Capstone.FOSSistant.global.web.dto.Member.MemberResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final GitHubOAuthClient gitHubOAuthClient;
    private final StringRedisTemplate redisTemplate;
    private final GitHubHelperService gitHubHelperService;

    @Override
    @Transactional(readOnly = true)
    public Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    @Override
    @Transactional
    public AuthResponseDTO.OAuthResponse loginOrSignUp(String githubCode) {
        // 1. code → access_token 요청
        String accessToken = gitHubOAuthClient.getAccessTokenFromCode(githubCode);

        // 2. access_token → 사용자 정보 조회
        GitHubOAuthClient.GitHubUserInfo userInfo = gitHubOAuthClient.getUserInfo(accessToken);
        String githubId     = String.valueOf(userInfo.githubId());
        String email        = (userInfo.email() == null || userInfo.email().isBlank())
                ? githubId + "@users.noreply.github.com"
                : userInfo.email();
        String nickname     = userInfo.nickname();
        String profileImage = userInfo.profileImageUrl();

        // 3. 사용자 조회 or 생성
        Member member = memberRepository.findByGithubId(githubId)
                .map(existing -> {
                    // 이미 가입된 유저면 프로필만 업데이트
                    existing.updateProfile(nickname, profileImage);
                    return existing;
                })
                .orElseGet(() -> {
                    // 신규 회원
                    return memberRepository.save(Member.builder()
                            .githubId(githubId)
                            .email(email)
                            .nickname(nickname)
                            .profileImage(profileImage)
                            .level(Level.BEGINNER)
                            .build());
                });

        updateTopLanguages(member, accessToken);

        // 4. JWT 발급 & Redis에 리프레시 토큰 저장
        String accessJwt  = jwtTokenProvider.createAccessToken(member.getMemberId());
        String refreshJwt = jwtTokenProvider.createRefreshToken(member.getMemberId());
        redisTemplate.opsForValue()
                .set("refresh:" + member.getMemberId(), refreshJwt, Duration.ofDays(7));

        return AuthResponseDTO.OAuthResponse.builder()
                .accessToken(accessJwt)
                .refreshToken(refreshJwt)
                .build();
    }

    @Override
    public String reissueAccessToken(String refreshToken) {
        if (!jwtTokenProvider.isTokenValid(refreshToken)) {
            throw new AuthException(ErrorStatus.AUTH_INVALID_TOKEN);
        }

        Long memberId = jwtTokenProvider.getId(refreshToken);
        String storedToken = redisTemplate.opsForValue().get("refresh:" + memberId);

        if (!refreshToken.equals(storedToken)) {
            throw new AuthException(ErrorStatus.AUTH_INVALID_TOKEN);
        }

        return jwtTokenProvider.createAccessToken(memberId);
    }

    @Override
    public void logout(Member member) {
        if (member.getMemberId() == null) {
            throw new MemberException(ErrorStatus.MEMBER_NOT_FOUND);
        }
        redisTemplate.delete("refresh:" + member.getMemberId());
    }

    @Override
    @Transactional
    public void withdraw(Member member) {
        String redisKey = "refresh:" + member.getMemberId();
        redisTemplate.delete(redisKey);

        if (member.getMemberId() == null) {
            throw new MemberException(ErrorStatus.MEMBER_NOT_FOUND);
        }
        memberRepository.delete(member);
    }

    @Override
    @Transactional
    public void updateLevel(Member member, Level level) {
        if (level == null) {
            throw new MemberException(ErrorStatus.MEMBER_LEVEL_BLANK);
        }

        Member found = memberRepository.findById(member.getMemberId())
                .orElseThrow(() -> new MemberException(ErrorStatus.MEMBER_NOT_FOUND));

        found.updateLevel(level);
    }


    @Override
    public AuthResponseDTO.OAuthResponse getServerAccessToken(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .githubId("DEV_" + memberId)          // dev용 식별자
                                .email("dev+" + memberId + "@example.com")
                                .nickname("dev" + memberId)
                                .profileImage("dev" + memberId)
                                .level(Level.BEGINNER)
                                .build()
                ));

        String access = jwtTokenProvider.createAccessToken(memberId);
        String refresh = jwtTokenProvider.createRefreshToken(memberId);

        // 기존 로직처럼 Redis에 저장
        redisTemplate.opsForValue()
                .set("refresh:" + memberId, refresh, Duration.ofDays(7));
        return AuthResponseDTO.OAuthResponse.builder()
                        .accessToken(access)
                        .refreshToken(refresh)
                        .build();
    }

    @Override
    public MemberResponseDTO.MemberProfileResponseDTO getProfile(Member member) {
        return MemberResponseDTO.MemberProfileResponseDTO.builder()
                .nickname(member.getNickname())
                .profileImage(member.getProfileImage())
                .level(member.getLevel())
                .build();
    }

    @Transactional
    public void updateTopLanguages(Member member, String accessToken) {
        Map<String, Long> langStats = gitHubHelperService.getUserLanguageStats(accessToken);

        List<String> supported = List.of(
                "Java","TypeScript","JavaScript","Python","Jupyter Notebook",
                "Swift","Kotlin","Ruby","C","C++","Go",
                "Fortran","R","PHP","Shell","Rust","HTML"
        );

        List<String> top3 = langStats.entrySet().stream()
                .filter(e -> supported.contains(e.getKey()))
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        member.updateTopLanguages(top3);

        memberRepository.save(member);
    }

}
