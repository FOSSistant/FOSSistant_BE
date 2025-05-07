package Capstone.FOSSistant.global.domain.entity;

import Capstone.FOSSistant.global.domain.common.BaseEntity;
import Capstone.FOSSistant.global.domain.enums.Tag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class IssueList extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Lob
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tag difficulty;
}
