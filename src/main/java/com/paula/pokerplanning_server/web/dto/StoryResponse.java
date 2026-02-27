package com.paula.pokerplanning_server.web.dto;

import com.paula.pokerplanning_server.domain.model.Story;
import com.paula.pokerplanning_server.domain.model.StoryStatus;

import java.time.Instant;
import java.util.UUID;

public record StoryResponse(
        UUID id,
        UUID roomId,
        String title,
        String description,
        String externalRef,
        int orderIndex,
        StoryStatus status,
        String finalEstimate,
        Instant createdAt
) {
    public static StoryResponse from(Story s) {
        return new StoryResponse(
                s.getId(),
                s.getRoomId(),
                s.getTitle(),
                s.getDescription(),
                s.getExternalRef(),
                s.getOrderIndex(),
                s.getStatus(),
                s.getFinalEstimate(),
                s.getCreatedAt()
        );
    }
}
