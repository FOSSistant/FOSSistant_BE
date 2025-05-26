package Capstone.FOSSistant.global.domain.entity;

import Capstone.FOSSistant.global.domain.common.BaseEntity;
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
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(length = 255, nullable = false, unique = true)
    private String email;

    @Column(length = 50, nullable = false, unique = true)
    private String githubId;

    @Column(length = 100, nullable = false)
    private String nickname;

    @Column(length = 512)
    private String profileImage;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Level level;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssueFeedback> feedbackList = new ArrayList<>();


    //로그인 정보 최신화
    public void updateProfile(String nickname, String profileImage) {
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

    public void updateLevel(Level newLevel) {
        this.level = newLevel;
    }
}
