package Capstone.FOSSistant.global.repository;

import Capstone.FOSSistant.global.domain.entity.IssueList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueListRepository extends JpaRepository<IssueList, String> {
}
