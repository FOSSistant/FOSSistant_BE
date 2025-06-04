package Capstone.FOSSistant.global.domain.entity;

import Capstone.FOSSistant.global.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GitHubRepository extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repository_id")
    private Long id;

    @Column(nullable = false)
    private String name;  // 레포지터리 이름

    @Column(nullable = false)
    private String fullName;  // owner/name 형태

    @Column(nullable = false)
    private String url;  // 레포지터리 URL

    @Column(nullable = true)
    private String language;  // 주 언어

    @Column(length = 1000)
    private String description;  // 레포 설명

    @Column
    private int stars;  // star 수
}