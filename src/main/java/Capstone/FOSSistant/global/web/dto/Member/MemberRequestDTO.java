package Capstone.FOSSistant.global.web.dto.Member;

import Capstone.FOSSistant.global.domain.enums.Level;
import Capstone.FOSSistant.global.web.dto.util.custom.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

public class MemberRequestDTO {
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
}
