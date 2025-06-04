package Capstone.FOSSistant.global.service.githubRepo;

import Capstone.FOSSistant.global.domain.entity.GitHubRepository;
import Capstone.FOSSistant.global.repository.GithubRepositoryRepository;
import Capstone.FOSSistant.global.service.GitHubHelperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubRepositoryServiceImpl implements GithubRepositoryService {

    private final GitHubHelperService gitHubHelperService;
    private final GithubRepositoryRepository gitHubRepositoryRepository;

    @Override
    public void fetchAndStoreTrendingRepositories() {
        List<String> languages = List.of("Java", "TypeScript", "JavaScript", "Python", "Jupyter Notebook");

        for (String language : languages) {
            List<GitHubRepository> trendingRepos = gitHubHelperService.fetchTrendingRepositories(language, 15);
            gitHubRepositoryRepository.saveAll(trendingRepos);
            log.info("[{}] 언어에 대해 {}개의 레포지터리 저장 완료", language, trendingRepos.size());
        }
    }
}
