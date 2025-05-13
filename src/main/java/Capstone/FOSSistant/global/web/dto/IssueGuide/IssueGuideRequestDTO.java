package Capstone.FOSSistant.global.web.dto.IssueGuide;

import Capstone.FOSSistant.global.web.dto.util.custom.ValidGitHubIssueUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueGuideRequestDTO {
    @ValidGitHubIssueUrl
    private String issueId;
}