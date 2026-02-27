package com.paula.pokerplanning_server.web.dto;

public record JoinRoomResponse(
        RoomResponse room,
        ParticipantResponse me,
        String guestToken  // null for authenticated users
) {}
