package Capstone.FOSSistant.global.repository;

import Capstone.FOSSistant.global.domain.entity.IssueFeedback;
import Capstone.FOSSistant.global.domain.entity.IssueList;
import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.domain.enums.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssueFeedbackRepository extends JpaRepository<IssueFeedback, Long> {
    Optional<IssueFeedback> findByMemberAndIssue(Member member, IssueList issue);
    Optional<IssueFeedback> findByMemberAndIssue_Id(Member member, String issueId);
}