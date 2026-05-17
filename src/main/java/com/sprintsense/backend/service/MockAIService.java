package com.sprintsense.backend.service;

import com.sprintsense.backend.dto.ActionItem;
import com.sprintsense.backend.dto.SprintAnalysisResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockAIService {

    public SprintAnalysisResponse analyzeSprint(String transcript) {
        // Mock response for testing
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
}
