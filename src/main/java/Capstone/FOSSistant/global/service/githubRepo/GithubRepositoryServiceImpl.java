package Capstone.FOSSistant.global.service.githubRepo;

import Capstone.FOSSistant.global.aop.annotation.MeasureExecutionTime;
import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;
import Capstone.FOSSistant.global.apiPayload.exception.GeneralException;
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

import java.util.Collections;
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
        // 기존 5개에 추가 12개를 더해 총 17개
        List<String> languages = List.of(
                "Java",
                "TypeScript",
                "JavaScript",
                "Python",
                "Jupyter Notebook",
                "Swift",
                "Kotlin",
                "Ruby",
                "C",
                "C++",
                "Go",
                "Fortran",
                "R",
                "PHP",
                "Shell",
                "Rust",
                "HTML"
        );

        int count = 5;  // 언어당 가져올 레포 수

        for (String language : languages) {
            List<GitHubRepository> trendingRepos =
                    gitHubHelperService.fetchTrendingRepositories(language, count);

            if (trendingRepos.isEmpty()) {
                log.warn("[{}] 언어에 대해 트렌딩 레포를 찾지 못했습니다.", language);
                continue;
            }

            gitHubRepositoryRepository.saveAll(trendingRepos);
            log.info("[{}] 언어에 대해 {}개의 트렌딩 레포 저장 완료", language, trendingRepos.size());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @MeasureExecutionTime
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

    @Override
    @MeasureExecutionTime
    public GithubRepoDTO.GithubRepoListDTO recommendByLanguage(String language) {
        List<GitHubRepository> repoList = gitHubRepositoryRepository.findTop15ByLanguage(language);

        if (repoList.isEmpty()) {
            throw new GeneralException(ErrorStatus.GITHUB_NO_REPOSITORY_FOUND);
        }

        Collections.shuffle(repoList);
        List<GitHubRepository> random3 = repoList.stream().limit(3).toList();

        List<GithubRepoDTO.GithubRepoResponseDTO> result = random3.stream()
                .map(repo -> GithubRepoDTO.GithubRepoResponseDTO.builder()
                        .name(repo.getName())
                        .fullName(repo.getFullName())
                        .url(repo.getUrl())
                        .description(repo.getDescription())
                        .language(repo.getLanguage())
                        .stars(repo.getStars())
                        .build())
                .toList();

        return GithubRepoDTO.GithubRepoListDTO.builder()
                .results(result)
                .build();
    }

}
