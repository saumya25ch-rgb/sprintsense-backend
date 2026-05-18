package com.sprintsense.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank
    private String question;

    private SprintAnalysisResponse analysisData;
}
