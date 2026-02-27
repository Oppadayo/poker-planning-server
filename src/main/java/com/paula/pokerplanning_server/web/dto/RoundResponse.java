package com.paula.pokerplanning_server.web.dto;

import com.paula.pokerplanning_server.domain.model.Round;
import com.paula.pokerplanning_server.domain.model.RoundStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoundResponse(
        UUID id,
        UUID storyId,
        RoundStatus status,
        Instant startedAt,
        Instant revealedAt,
        Instant finalizedAt,
        List<VoteResponse> votes
) {
    public static RoundResponse from(Round r, List<VoteResponse> votes) {
        return new RoundResponse(
                r.getId(),
                r.getStoryId(),
                r.getStatus(),
                r.getStartedAt(),
                r.getRevealedAt(),
                r.getFinalizedAt(),
                votes
        );
    }
}
