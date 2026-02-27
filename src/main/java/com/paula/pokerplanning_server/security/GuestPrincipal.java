package com.paula.pokerplanning_server.security;

import com.paula.pokerplanning_server.domain.model.ParticipantRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.Principal;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class GuestPrincipal implements Principal {

    private final String guestId;
    private final UUID participantId;
    private final UUID roomId;
    private final ParticipantRole role;

    @Override
    public String getName() {
        return "guest:" + guestId;
    }
}
