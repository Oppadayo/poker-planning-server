package com.paula.pokerplanning_server.web.dto;

import java.util.List;
import java.util.UUID;

public record RoomStateResponse(
        RoomResponse room,
        ParticipantResponse me,
        List<ParticipantResponse> participants,
        List<StoryResponse> stories,
        UUID currentStoryId,
        RoundResponse round  // null if no active round
) {}
