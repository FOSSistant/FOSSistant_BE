package Capstone.FOSSistant.global.domain.entity;

import Capstone.FOSSistant.global.domain.enums.Level;
import Capstone.FOSSistant.global.domain.enums.Tag;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(length = 255, nullable = false, unique = true)
    private String email;

    @Column(length = 50, nullable = false, unique = true)
    private String githubId;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Level level;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssueFeedback> feedbackList = new ArrayList<>();
}
