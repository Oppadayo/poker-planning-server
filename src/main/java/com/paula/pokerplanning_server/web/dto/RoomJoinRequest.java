package com.paula.pokerplanning_server.web.dto;

import com.paula.pokerplanning_server.domain.model.ParticipantRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomJoinRequest(
        @NotBlank @Size(max = 100) String displayName,
        ParticipantRole role
) {}
