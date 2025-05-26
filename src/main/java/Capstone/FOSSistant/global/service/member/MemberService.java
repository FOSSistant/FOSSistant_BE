package Capstone.FOSSistant.global.service.member;

import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.domain.enums.Level;
import Capstone.FOSSistant.global.web.dto.Member.AuthResponseDTO;
import Capstone.FOSSistant.global.web.dto.Member.MemberResponseDTO;

public interface MemberService {
    Member findMemberById(Long memberId);
    AuthResponseDTO.OAuthResponse loginOrSignUp(String githubCode);
    String reissueAccessToken(String refreshToken);
    void logout(Member member);
    void withdraw(Member member);
    void updateLevel(Member member, Level level);
    AuthResponseDTO.OAuthResponse getServerAccessToken(Long memberId);
    MemberResponseDTO.MemberProfileResponseDTO getProfile(Member member);
}
