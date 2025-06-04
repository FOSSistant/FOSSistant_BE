package Capstone.FOSSistant.global.service.githubRepo;

import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.web.dto.GithubRepo.GithubRepoDTO;

public interface GithubRepositoryService {
    void fetchAndStoreTrendingRepositories();

    GithubRepoDTO.GithubRepoListDTO recommendTopRepositories(Member member);
}


