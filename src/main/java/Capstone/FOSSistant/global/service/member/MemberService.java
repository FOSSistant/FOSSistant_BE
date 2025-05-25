package Capstone.FOSSistant.global.service.member;

import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.web.dto.Member.AuthResponseDTO;

public interface MemberService {
    Member findMemberById(Long memberId);
    AuthResponseDTO.OAuthResponse loginOrSignUp(String githubCode);
    String reissueAccessToken(String refreshToken);
    void logout(Member member);
    void withdraw(Member member);
}
