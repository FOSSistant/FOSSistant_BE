package Capstone.FOSSistant.global.domain.entity;

import Capstone.FOSSistant.global.domain.enums.Tag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id", "issue_id"})
        }
)
public class IssueFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private IssueList issue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Tag feedbackTag;
}