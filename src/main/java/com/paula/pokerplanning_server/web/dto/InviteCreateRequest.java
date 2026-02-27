package com.paula.pokerplanning_server.web.dto;

import com.paula.pokerplanning_server.domain.model.ParticipantRole;

import java.time.Instant;

public record InviteCreateRequest(
        ParticipantRole role,
        Instant expiresAt,
        Integer maxUses
) {}
