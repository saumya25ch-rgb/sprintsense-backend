package com.sprintsense.backend.controller;

import com.sprintsense.backend.dto.ChatRequest;
import com.sprintsense.backend.dto.SprintAnalysisResponse;
import com.sprintsense.backend.service.MockAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AIController {
    private final MockAIService mockAIService;

    @PostMapping("/analyze")
    public SprintAnalysisResponse analyzeSprint(@RequestBody String transcript) {
        return mockAIService.analyzeSprint(transcript);
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest chatRequest) {
        String question = chatRequest.getQuestion().toLowerCase();
        SprintAnalysisResponse analysisResponse = chatRequest.getAnalysisData();
        if(analysisResponse == null) {
            return "No sprint analysis data available. Please analyze the sprint first.";
        }

        //Blockers
        if (question.equals("blocker")) {
            return String.join(", ", analysisResponse.getBlockers());
        }

        //Risks
        if (question.equals("risk")) {
            return String.join(", ", analysisResponse.getRisks());
        }

        //Recommendations
        if (question.equals("delay")) {
            return String.join(", ", analysisResponse.getRecommendations());
        }

        //Action Items
        if (question.equals("action")) {
            StringBuilder builder = new StringBuilder();
            analysisResponse.getActionItems().forEach(item -> builder.append(item.getTask())
                    .append(" (Assigned to: ").append(item.getOwner())
                    .append(", Priority: ").append(item.getPriority()).append(")\n"));
            return builder.toString();
        }

        return analysisResponse.getSummary();
    }
}
