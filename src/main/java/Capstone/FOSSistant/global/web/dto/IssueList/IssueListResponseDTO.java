package Capstone.FOSSistant.global.web.dto.IssueList;

import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueListResponseDTO {

    private List<IssueResponseDTO> results;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IssueResponseDTO {
        private String issueId;
        private String difficulty;
    }
}