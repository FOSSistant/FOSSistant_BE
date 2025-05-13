package Capstone.FOSSistant.global.web.dto.IssueList;


import Capstone.FOSSistant.global.web.dto.util.custom.ValidGitHubIssueUrl;
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
    public static class IssueRequestDTO{
        @ValidGitHubIssueUrl
        String issueId;
    }
}
