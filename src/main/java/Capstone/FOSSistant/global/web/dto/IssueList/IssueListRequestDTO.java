package Capstone.FOSSistant.global.web.dto.IssueList;


import Capstone.FOSSistant.global.web.dto.util.custom.ValidGitHubIssueUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueListRequestDTO {
    @Valid
    private List<IssueRequestDTO> issues;

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Schema(description = "이슈 요청 DTO")
    public static class IssueRequestDTO{
        @ValidGitHubIssueUrl
        @Schema(description = "GitHub 이슈 URL", example = "https://github.com/org/repo/issues/123")
        String issueId;
    }
}
