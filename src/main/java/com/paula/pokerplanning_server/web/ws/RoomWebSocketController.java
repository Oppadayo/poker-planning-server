package com.paula.pokerplanning_server.web.ws;

import com.paula.pokerplanning_server.security.ActorContext;
import com.paula.pokerplanning_server.security.GuestPrincipal;
import com.paula.pokerplanning_server.security.UserPrincipal;
import com.paula.pokerplanning_server.service.RoundService;
import com.paula.pokerplanning_server.web.dto.VoteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * Handles STOMP messages sent from clients to the server.
 * Clients publish to /app/rooms/{roomId}/vote.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomWebSocketController {

    private final RoundService roundService;

    /**
     * Cast a vote via WebSocket.
     * Clients send to: /app/rooms/{roomId}/vote
     */
    @MessageMapping("/rooms/{roomId}/vote")
    public void castVote(
            @DestinationVariable UUID roomId,
            @Payload VoteRequest voteRequest,
            Principal principal) {

        ActorContext actor = resolveActor(principal);
        if (actor == null) {
            log.warn("Unauthenticated WebSocket vote attempt for room {}", roomId);
            return;
        }

        roundService.castVote(roomId, actor, voteRequest.value());
    }

    private ActorContext resolveActor(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            Object p = token.getPrincipal();
            if (p instanceof UserPrincipal user) {
                return ActorContext.forUser(user.getUserId());
            }
            if (p instanceof GuestPrincipal guest) {
                return ActorContext.forGuestWithToken(guest.getGuestId(), guest.getParticipantId(), guest.getRole());
            }
        }
        return null;
    }
}
