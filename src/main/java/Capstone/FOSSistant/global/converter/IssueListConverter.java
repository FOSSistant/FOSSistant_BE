package Capstone.FOSSistant.global.converter;

import Capstone.FOSSistant.global.domain.entity.IssueList;
import Capstone.FOSSistant.global.domain.enums.Tag;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListResponseDTO;

public class IssueListConverter {
    public static IssueList toEntity(IssueListRequestDTO.IssueRequestDTO dto, Tag difficulty) {
        return IssueList.builder()
                .id(dto.getIssueId())
                .difficulty(difficulty)
                .build();
    }

    public static IssueListResponseDTO.IssueResponseDTO toResponseDTO(String id, Tag difficulty) {
        return IssueListResponseDTO.IssueResponseDTO.builder()
                .issueId(id)
                .difficulty(difficulty.name().toLowerCase())
                .build();
    }
}
