package com.paula.pokerplanning_server.security;

import com.paula.pokerplanning_server.domain.model.ParticipantRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class GuestTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public GuestTokenProvider(
            @Value("${app.guest-token.secret}") String secret,
            @Value("${app.guest-token.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String guestId, UUID participantId, UUID roomId, ParticipantRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(guestId)
                .claim("participantId", participantId.toString())
                .claim("roomId", roomId.toString())
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    public record GuestClaims(String guestId, UUID participantId, UUID roomId, ParticipantRole role) {}

    public GuestClaims validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new GuestClaims(
                    claims.getSubject(),
                    UUID.fromString(claims.get("participantId", String.class)),
                    UUID.fromString(claims.get("roomId", String.class)),
                    ParticipantRole.valueOf(claims.get("role", String.class))
            );
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid guest token: {}", e.getMessage());
            throw new com.paula.pokerplanning_server.exception.ForbiddenException("Invalid or expired guest token");
        }
    }

    public GuestClaims validateForRoom(String token, UUID expectedRoomId) {
        GuestClaims claims = validate(token);
        if (!claims.roomId().equals(expectedRoomId)) {
            throw new com.paula.pokerplanning_server.exception.ForbiddenException("Guest token not valid for this room");
        }
        return claims;
    }
}
