package com.sprintsense.backend.service;

import com.sprintsense.backend.dto.ActionItem;
import com.sprintsense.backend.dto.SprintAnalysisResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("mock")
public class MockAIService implements AnalysisService {

    @Override
    public SprintAnalysisResponse analyze(String transcript) {
        return new SprintAnalysisResponse(
            "Sprint was successful with good team collaboration.",
            List.of("Delayed API integration", "Unclear requirements from product owner"),
            List.of("Potential burnout due to tight deadlines"),
            List.of("Improve communication with product owner", "Allocate more time for testing"),
            List.of(
                new ActionItem("Schedule meeting with product owner", "John Doe", "High"),
                new ActionItem("Conduct team retrospective", "Jane Smith", "Medium")
            )
        );
    }

    @Override
    public String chat(String question, SprintAnalysisResponse context) {
        if (context == null) {
            return "No sprint analysis data available. Please analyze the sprint first.";
        }
        String q = question == null ? "" : question.toLowerCase();
        if (q.contains("blocker")) return String.join(", ", context.getBlockers());
        if (q.contains("risk")) return String.join(", ", context.getRisks());
        if (q.contains("recommend") || q.contains("delay")) return String.join(", ", context.getRecommendations());
        if (q.contains("action") || q.contains("task")) {
            StringBuilder b = new StringBuilder();
            context.getActionItems().forEach(item -> b.append("- ").append(item.getTask())
                    .append(" (Owner: ").append(item.getOwner())
                    .append(", Priority: ").append(item.getPriority()).append(")\n"));
            return b.toString();
        }
        return context.getSummary();
    }
}
