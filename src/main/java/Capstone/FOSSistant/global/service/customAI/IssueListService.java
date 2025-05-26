package Capstone.FOSSistant.global.service.customAI;

import Capstone.FOSSistant.global.web.dto.IssueList.IssueListRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListResponseDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IssueListService {
//    CompletableFuture<IssueListResponseDTO.IssueResponseDTO> classify(IssueListRequestDTO.IssueRequestDTO dto);

    List<CompletableFuture<IssueListResponseDTO.IssueResponseDTO>> classifyAll(List<IssueListRequestDTO.IssueRequestDTO> dtoList);
}