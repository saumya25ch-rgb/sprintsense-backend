package com.sprintsense.backend.service;

import com.sprintsense.backend.dto.SprintAnalysisResponse;

public interface AnalysisService {
    SprintAnalysisResponse analyze(String transcript);
    String chat(String question, SprintAnalysisResponse context);
}
