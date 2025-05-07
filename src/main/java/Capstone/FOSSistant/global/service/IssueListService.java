package Capstone.FOSSistant.global.service;

import Capstone.FOSSistant.global.web.dto.IssueList.IssueListRequestDTO;
import Capstone.FOSSistant.global.web.dto.IssueList.IssueListResponseDTO;

import java.util.concurrent.CompletableFuture;

public interface IssueListService {
    CompletableFuture<IssueListResponseDTO.IssueResponseDTO> classify(IssueListRequestDTO.IssueRequestDTO dto);
}
