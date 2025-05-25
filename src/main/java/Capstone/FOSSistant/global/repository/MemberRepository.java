package Capstone.FOSSistant.global.repository;

import Capstone.FOSSistant.global.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // GitHub ID 기반 조회
    Optional<Member> findByGithubId(String githubId);

    // 이메일로 조회
    Optional<Member> findByEmail(String email);

    // GitHub ID 중복 여부 체크
    boolean existsByGithubId(String githubId);
}
