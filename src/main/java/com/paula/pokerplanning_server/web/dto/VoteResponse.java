package com.paula.pokerplanning_server.web.dto;

import java.util.UUID;

public record VoteResponse(
        UUID participantId,
        boolean hasVoted,
        String value  // null during VOTING phase
) {}
