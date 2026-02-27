package com.paula.pokerplanning_server.web.dto;

import com.paula.pokerplanning_server.domain.model.Participant;
import com.paula.pokerplanning_server.domain.model.ParticipantRole;

import java.util.UUID;

public record ParticipantResponse(
        UUID id,
        ParticipantRole role,
        String displayName,
        boolean online
) {
    public static ParticipantResponse from(Participant p) {
        return new ParticipantResponse(p.getId(), p.getRole(), p.getDisplayName(), p.isOnline());
    }
}
