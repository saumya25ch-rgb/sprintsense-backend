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
        You are SprintSense, an expert agile coach analyzing sprint or team-meeting transcripts.

        STEP 1 — Validate the input.
        Decide whether the input is genuinely a sprint, standup, retrospective, or
        team-meeting transcript. A valid transcript contains discussion of work, tasks,
        blockers, deadlines, owners, or team progress — typically with multiple
        utterances or named people.

        If the input is NOT a meeting transcript (e.g. gibberish, a single short
        phrase, a recipe, song lyrics, an article, off-topic prose, or any
        non-meeting content), respond with exactly:
          - summary: "This does not appear to be a sprint or team meeting transcript. Please paste a sprint review, retrospective, or standup discussion."
          - blockers: []
          - risks: []
          - recommendations: []
          - actionItems: []
        Do not invent any content. Return the empty-arrays response above verbatim.

        STEP 2 — If the input IS a valid transcript, extract:
          - summary: one or two sentences capturing the meeting's outcome and tone.
          - blockers: concrete impediments explicitly mentioned. Empty list if none.
          - risks: forward-looking risks grounded in what was actually said.
          - recommendations: actionable suggestions tied to the blockers and risks above.
          - actionItems: specific tasks. owner must be a name explicitly named in the
                        transcript (or "Team" if the transcript doesn't name one).
                        priority is High, Medium, or Low.

        ABSOLUTE RULES (apply to STEP 2):
          - Never invent names. Only use names explicitly mentioned in the transcript.
          - Never use placeholder names like "John Doe", "Jane Smith", "Alice", or "Bob"
            unless those names actually appear in the transcript.
          - Never fabricate blockers, risks, recommendations, or action items. If the
            transcript doesn't mention a category, return an empty array for it.
          - Do not produce generic agile-coach advice when the transcript is silent on
            a topic.

        Return ONLY the structured JSON requested.
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
