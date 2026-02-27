package com.paula.pokerplanning_server.security;

import com.paula.pokerplanning_server.domain.model.ParticipantRole;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final GuestTokenProvider guestTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        Principal principal = resolveFromHeaders(accessor);
        if (principal != null) {
            accessor.setUser(principal);
        }

        return message;
    }

    private Principal resolveFromHeaders(StompHeaderAccessor accessor) {
        // Try JWT first (user accounts)
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtTokenProvider.validateAndParseClaims(token);
                UserPrincipal userPrincipal = jwtTokenProvider.toPrincipal(claims);
                return new UsernamePasswordAuthenticationToken(userPrincipal, null, List.of());
            } catch (Exception e) {
                log.debug("STOMP JWT validation failed: {}", e.getMessage());
            }
        }

        // Try guest token
        String guestToken = accessor.getFirstNativeHeader("X-Guest-Token");
        if (guestToken != null) {
            try {
                GuestTokenProvider.GuestClaims claims = guestTokenProvider.validate(guestToken);
                GuestPrincipal guestPrincipal = new GuestPrincipal(
                        claims.guestId(),
                        claims.participantId(),
                        claims.roomId(),
                        claims.role()
                );
                return new UsernamePasswordAuthenticationToken(guestPrincipal, null, List.of());
            } catch (Exception e) {
                log.debug("STOMP guest token validation failed: {}", e.getMessage());
            }
        }

        return null;
    }
}
