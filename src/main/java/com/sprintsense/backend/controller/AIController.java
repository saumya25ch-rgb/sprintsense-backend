package com.sprintsense.backend.controller;

import com.sprintsense.backend.dto.ChatRequest;
import com.sprintsense.backend.dto.SprintAnalysisResponse;
import com.sprintsense.backend.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {
    private final AnalysisService analysisService;

    @PostMapping("/analyze")
    public SprintAnalysisResponse analyzeSprint(@RequestBody String transcript) {
        return analysisService.analyze(transcript);
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest chatRequest) {
        return analysisService.chat(chatRequest.getQuestion(), chatRequest.getAnalysisData());
    }
}
