package Capstone.FOSSistant.global.web.dto.Member;

import Capstone.FOSSistant.global.domain.enums.Level;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class AuthRequestDTO {

    @Getter
    public static class RefreshToken {
        @JsonProperty("refreshToken")
        String refreshToken;
    }

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class GithubRequest {

        @NotBlank(message = "깃헙 인증 코드를 입력해주세요")
        private String githubCode;
    }

    @Getter
    public static class UpdateLevelRequest {
        private Level level;
    }

}
