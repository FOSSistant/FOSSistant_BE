package Capstone.FOSSistant.global.web.dto.IssueList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassificationRequest {
    private String title;
    private String body;
}