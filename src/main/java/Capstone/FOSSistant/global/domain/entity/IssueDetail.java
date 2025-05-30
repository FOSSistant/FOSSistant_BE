package Capstone.FOSSistant.global.domain.entity;

import Capstone.FOSSistant.global.domain.common.BaseEntity;
import Capstone.FOSSistant.global.domain.enums.Tag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class IssueDetail extends BaseEntity {

    @Id
    @Column(nullable = false, name = "issue_detail_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false, unique = true)
    private IssueList issue;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(length = 1000,nullable = false)
    private String highlightedBody;

    @Lob
    @Column(length = 1000,nullable = false)
    private String description;

    @Lob
    @Column(length = 1000,nullable = false)
    private String solution;

    @Lob
    @Column(length = 1000,nullable = false)
    private String relatedLinks;

    public Tag getTag() {
        return issue.getDifficulty();
    }
}