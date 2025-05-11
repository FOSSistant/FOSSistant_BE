package Capstone.FOSSistant.global.web.dto.IssueGuide;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class IssueGuideResponseDTO {
    private String title;
    private String difficulty;
    private String description;
    private String solution;
    private String caution;
}