package Capstone.FOSSistant.global.web.dto.IssueGuide;

import Capstone.FOSSistant.global.domain.enums.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueGuideResponseDTO {
    private String title;
    private String difficulty;
    private String highlightedBody;
    private String description;
    private String solution;
    private String relatedLinks;
}