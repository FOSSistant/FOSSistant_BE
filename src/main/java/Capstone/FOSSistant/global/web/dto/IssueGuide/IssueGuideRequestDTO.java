package Capstone.FOSSistant.global.web.dto.IssueGuide;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class IssueGuideRequestDTO {
    private String issueId;
    private String title;
    private String body;
}