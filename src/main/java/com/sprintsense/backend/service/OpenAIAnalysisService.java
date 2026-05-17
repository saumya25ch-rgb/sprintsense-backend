package com.sprintsense.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintsense.backend.dto.SprintAnalysisResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Calls an OpenAI-compatible chat completions endpoint. Works against both
 * the OpenAI API directly and any compatible service (GitHub Models, Azure
 * OpenAI via OpenAI-compat layer, local OpenAI-compat servers). The actual
 * endpoint and credentials are selected by which application-{profile}.properties
 * is loaded.
 */
@Service
@Profile("!mock")
public class OpenAIAnalysisService implements AnalysisService {

    private static final String ANALYZE_SYSTEM_PROMPT = """
        You are SprintSense, an expert agile coach analyzing sprint retrospective transcripts.
        Read the transcript carefully and extract:
          - summary: one or two sentences capturing the sprint's overall outcome and tone.
          - blockers: concrete impediments the team hit (not generic worries). Empty list if none.
          - risks: forward-looking risks for the next sprint based on what was said.
          - recommendations: actionable suggestions tied to the blockers and risks.
          - actionItems: specific tasks with an owner (name mentioned in transcript, or "Team" if unclear)
                        and priority (High/Medium/Low).
        Be specific. Quote names and topics from the transcript. Do not invent facts.
        Return ONLY the structured JSON requested by the response format.
        """;

    private static final String CHAT_SYSTEM_PROMPT_TEMPLATE = """
        You are SprintSense, a helpful assistant answering follow-up questions about a sprint
        retrospective analysis. Answer ONLY using the analysis JSON below. If the analysis does not
        contain the information needed, say so plainly. Keep answers concise (2-4 sentences unless
        the user asks for detail).

        Sprint Analysis JSON:
        %s
        """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public OpenAIAnalysisService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public SprintAnalysisResponse analyze(String transcript) {
        return chatClient.prompt()
                .system(ANALYZE_SYSTEM_PROMPT)
                .user(transcript)
                .call()
                .entity(SprintAnalysisResponse.class);
    }

    @Override
    public String chat(String question, SprintAnalysisResponse context) {
        if (context == null) {
            return "No sprint analysis data available. Please analyze the sprint first.";
        }
        String contextJson = serialize(context);
        return chatClient.prompt()
                .system(CHAT_SYSTEM_PROMPT_TEMPLATE.formatted(contextJson))
                .user(question)
                .call()
                .content();
    }

    private String serialize(SprintAnalysisResponse context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize analysis context", e);
        }
    }
}
