package com.sprintsense.backend.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private SprintAnalysisResponse analysisData;
}
