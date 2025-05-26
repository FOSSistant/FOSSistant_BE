package Capstone.FOSSistant.global.web.dto.IssueGuide;

import Capstone.FOSSistant.global.domain.enums.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

public class IssueFeedbackRequestDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Feedback {

        @NotBlank(message = "issueId는 필수입니다.")
        private String issueId;

        @NotNull(message = "feedbackTag는 필수입니다.")
        private Tag feedbackTag;
    }



}