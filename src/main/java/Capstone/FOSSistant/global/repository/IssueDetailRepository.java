package Capstone.FOSSistant.global.repository;

import Capstone.FOSSistant.global.domain.entity.IssueDetail;
import Capstone.FOSSistant.global.domain.entity.IssueList;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssueDetailRepository extends JpaRepository<IssueDetail, Long> {
    @EntityGraph(attributePaths = {"issue"})
    Optional<IssueDetail> findByIssue_Id(String issueId);
}