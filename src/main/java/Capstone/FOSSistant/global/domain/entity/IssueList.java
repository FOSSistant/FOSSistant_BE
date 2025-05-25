package Capstone.FOSSistant.global.domain.entity;

import Capstone.FOSSistant.global.domain.common.BaseEntity;
import Capstone.FOSSistant.global.domain.enums.Tag;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class IssueList extends BaseEntity {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Tag difficulty;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssueFeedback> feedbackList = new ArrayList<>();

}
