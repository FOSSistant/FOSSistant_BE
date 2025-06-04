package Capstone.FOSSistant.global.web.dto.GithubRepo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

public class GithubRepoDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GithubRepoResponseDTO {
        private String name;
        private String fullName;
        private String url;
        private String description;
        private String language;
        private Integer stars;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GithubRepoListDTO {
        private List<GithubRepoResponseDTO> results;
    }
}
