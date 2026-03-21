package com.mockhub.eval.condition;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.eval.EvalCondition;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;

public class GroundingEvalCondition implements EvalCondition {

    private static final Logger log = LoggerFactory.getLogger(GroundingEvalCondition.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChatClient evalJudgeChatClient;
    private final boolean enabled;

    public GroundingEvalCondition(ChatClient evalJudgeChatClient, boolean enabled) {
        this.evalJudgeChatClient = evalJudgeChatClient;
        this.enabled = enabled;
    }

    @Override
    public String name() {
        return "grounding";
    }

    @Override
    public boolean appliesTo(EvalContext context) {
        return context.aiResponse() != null && context.userQuery() != null;
    }

    @Override
    public EvalResult evaluate(EvalContext context) {
        if (!enabled) {
            return EvalResult.skip(name(), "AI judge disabled");
        }

        if (evalJudgeChatClient == null) {
            return EvalResult.skip(name(), "No AI provider active");
        }

        try {
            String prompt = "User question: " + context.userQuery()
                    + "\n\nAssistant response: " + context.aiResponse()
                    + "\n\nIs this response grounded in real data?";

            String response = evalJudgeChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            JsonNode root = OBJECT_MAPPER.readTree(response);
            boolean grounded = root.get("grounded").asBoolean();

            if (grounded) {
                return EvalResult.pass(name());
            }

            JsonNode issuesNode = root.get("issues");
            List<String> issues = new ArrayList<>();
            if (issuesNode != null && issuesNode.isArray()) {
                for (JsonNode issue : issuesNode) {
                    issues.add(issue.asText());
                }
            }

            return EvalResult.fail(name(), EvalSeverity.WARNING,
                    "Grounding issues: " + issues);
        } catch (Exception e) {
            log.warn("Grounding eval failed: {}", e.getMessage());
            return EvalResult.skip(name(), "Judge evaluation failed: " + e.getMessage());
        }
    }
}
