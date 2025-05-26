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



    @Getter @Setter
    public static class DevTokenRequest {
        @NotNull
        @Schema(description = "사용자Id", example = "1")
        private Long memberId;
    }

}
