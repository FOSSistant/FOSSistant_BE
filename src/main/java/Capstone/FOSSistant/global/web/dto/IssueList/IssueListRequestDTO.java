package Capstone.FOSSistant.global.web.dto.IssueList;


import lombok.*;

import java.util.List;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueListRequestDTO {
    private List<IssueRequestDTO> issues;

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class IssueRequestDTO{
        String issueId;
    }
}
