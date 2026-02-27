package com.paula.pokerplanning_server.service;

import com.paula.pokerplanning_server.domain.model.ParticipantRole;
import com.paula.pokerplanning_server.exception.ForbiddenException;
import com.paula.pokerplanning_server.security.ActorContext;
import com.paula.pokerplanning_server.security.GuestTokenProvider;
import com.paula.pokerplanning_server.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActorService {

    private final GuestTokenProvider guestTokenProvider;

    /**
     * Resolves the actor for regular (non-host) operations.
     * For users: uses JWT auth from Spring Security context.
     * For guests: uses X-Guest-Id header.
     */
    public ActorContext resolve(Authentication auth, String guestId) {
        if (isAuthenticated(auth)) {
            UserPrincipal principal = extractUserPrincipal(auth);
            return ActorContext.forUser(principal.getUserId());
        }
        if (StringUtils.hasText(guestId)) {
            return ActorContext.forGuest(guestId);
        }
        throw new ForbiddenException("Authentication required: provide JWT or X-Guest-Id header");
    }

    /**
     * Resolves the actor for host-only operations.
     * For users: validates JWT auth.
     * For guests: validates the signed guest token (which carries the role claim).
     */
    public ActorContext resolveHost(Authentication auth, String guestToken, UUID roomId) {
        if (isAuthenticated(auth)) {
            UserPrincipal principal = extractUserPrincipal(auth);
            return ActorContext.forUser(principal.getUserId());
        }
        if (StringUtils.hasText(guestToken)) {
            GuestTokenProvider.GuestClaims claims = guestTokenProvider.validateForRoom(guestToken, roomId);
            if (claims.role() != ParticipantRole.HOST) {
                throw new ForbiddenException("Only host can perform this action");
            }
            return ActorContext.forGuestWithToken(claims.guestId(), claims.participantId(), claims.role());
        }
        throw new ForbiddenException("Host authentication required: provide JWT or X-Guest-Token header");
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth instanceof UsernamePasswordAuthenticationToken token
                && token.isAuthenticated()
                && token.getPrincipal() instanceof UserPrincipal;
    }

    private UserPrincipal extractUserPrincipal(Authentication auth) {
        return (UserPrincipal) ((UsernamePasswordAuthenticationToken) auth).getPrincipal();
    }
}
