package Capstone.FOSSistant.global.web.dto.IssueList;

import lombok.*;

import java.util.List;

@Builder
public class IssueListResponseDTO {

    private List<IssueResponseDTO> results;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IssueResponseDTO {
        private String id;
        private String difficulty;
    }
}