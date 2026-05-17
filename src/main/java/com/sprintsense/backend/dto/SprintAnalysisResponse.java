package com.sprintsense.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SprintAnalysisResponse {
    private String summary;
    private List<String> blockers;
    private List<String> risks;
    private List<String> recommendations;
    private List<ActionItem> actionItems;
}
