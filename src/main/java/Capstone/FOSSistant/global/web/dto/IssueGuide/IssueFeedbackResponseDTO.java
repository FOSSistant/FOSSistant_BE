package Capstone.FOSSistant.global.web.dto.IssueGuide;

import Capstone.FOSSistant.global.domain.enums.Tag;
import lombok.*;

public class IssueFeedbackResponseDTO {
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Feedback {
        private String issueId;
        private Tag   feedbackTag;
    }
}