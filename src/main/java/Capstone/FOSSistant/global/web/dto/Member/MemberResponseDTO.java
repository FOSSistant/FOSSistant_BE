package Capstone.FOSSistant.global.web.dto.Member;

import Capstone.FOSSistant.global.domain.enums.Level;
import lombok.*;

public class MemberResponseDTO {

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class MemberProfileResponseDTO {
        private String nickname;
        private String profileImage;
        private Level level;
    }
}
