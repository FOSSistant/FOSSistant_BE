package Capstone.FOSSistant.global.web.dto.IssueList;


import lombok.*;

import java.util.List;

@Getter
public class IssueListRequestDTO {
    private List<IssueRequestDTO> issues;

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class IssueRequestDTO{
        String id;
        String title;
        String body;
    }
}
