package com.mockhub.ai.dto;

import java.time.Instant;

public record ChatResponse(
        Long conversationId,
        String message,
        Instant timestamp
) {
}
