package Capstone.FOSSistant.global.service.member;

import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.exception.AuthException;
import Capstone.FOSSistant.global.apiPayload.exception.MemberException;
import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.domain.enums.Level;
import Capstone.FOSSistant.global.repository.MemberRepository;
import Capstone.FOSSistant.global.security.oauth.GitHubOAuthClient;
import Capstone.FOSSistant.global.security.provider.JwtTokenProvider;
import Capstone.FOSSistant.global.web.dto.Member.AuthResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final GitHubOAuthClient gitHubOAuthClient;
    private final StringRedisTemplate redisTemplate;

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

        String githubId = String.valueOf(userInfo.githubId());
        String email = (userInfo.email() == null || userInfo.email().isBlank())
                ? githubId + "@users.noreply.github.com"
                : userInfo.email();

        // 3. 사용자 조회 or 생성
        Member member = memberRepository.findByGithubId(githubId)
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .githubId(githubId)
                                .email(email)
                                .level(Level.BEGINNER)
                                .build()
                ));

        // 4. JWT 발급
        if (member.getMemberId() == null) {
            throw new MemberException(ErrorStatus.MEMBER_NOT_FOUND); // 또는 다른 예외
        }
        String accessJwt = jwtTokenProvider.createAccessToken(member.getMemberId());
        String refreshJwt = jwtTokenProvider.createRefreshToken(member.getMemberId());

        String key = "refresh:" + member.getMemberId();
        redisTemplate.opsForValue().set(key, refreshJwt, Duration.ofDays(7));

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
}
