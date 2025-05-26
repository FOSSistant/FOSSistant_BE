package Capstone.FOSSistant.global.web.dto.Member;

import Capstone.FOSSistant.global.domain.enums.Level;
import Capstone.FOSSistant.global.web.dto.util.custom.EnumValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @Builder
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class UpdateLevelRequest {
        @Schema(description = "사용자의 level", example = "BEGINNER 또는 EXPERIENCED")
        @NotNull
        @EnumValue(enumClass = Level.class, message = "level에 유효한 값만 입력할 수 있습니다.")
        private Level level;
    }

    @Getter @Setter
    public static class DevTokenRequest {
        @NotNull
        private Long memberId;
    }

}
