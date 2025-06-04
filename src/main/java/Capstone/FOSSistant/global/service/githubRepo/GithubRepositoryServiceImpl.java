package Capstone.FOSSistant.global.service.githubRepo;

import Capstone.FOSSistant.global.domain.entity.GitHubRepository;
import Capstone.FOSSistant.global.domain.entity.Member;
import Capstone.FOSSistant.global.domain.entity.MemberTopLanguage;
import Capstone.FOSSistant.global.repository.GithubRepositoryRepository;
import Capstone.FOSSistant.global.service.GitHubHelperService;
import Capstone.FOSSistant.global.web.dto.GithubRepo.GithubRepoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

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

    @Override
    @Transactional(readOnly = true)
    public GithubRepoDTO.GithubRepoListDTO recommendTopRepositories(Member member) {

        List<MemberTopLanguage> topLanguages = member.getTopLanguages();

        List<GithubRepoDTO.GithubRepoResponseDTO> dtoList = topLanguages.stream()
                .map(MemberTopLanguage::getLanguage)
                .map(language -> {
                    List<GitHubRepository> repos = gitHubRepositoryRepository.findAllByLanguage(language);
                    if (repos.isEmpty()) return null;

                    GitHubRepository selected = repos.get((int) (Math.random() * repos.size()));
                    return toDto(selected);
                })
                .filter(Objects::nonNull)
                .toList();

        return GithubRepoDTO.GithubRepoListDTO.builder().results(dtoList).build();
    }


    private GithubRepoDTO.GithubRepoResponseDTO toDto(GitHubRepository repo) {
        return GithubRepoDTO.GithubRepoResponseDTO.builder()
                .name(repo.getName())
                .fullName(repo.getFullName())
                .url(repo.getUrl())
                .description(repo.getDescription())
                .language(repo.getLanguage())
                .stars(repo.getStars())
                .build();
    }
}
