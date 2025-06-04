package Capstone.FOSSistant.global.repository;

import Capstone.FOSSistant.global.domain.entity.GitHubRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubRepositoryRepository extends JpaRepository<GitHubRepository, Long> {
}
