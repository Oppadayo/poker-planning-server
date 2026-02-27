package com.paula.pokerplanning_server.service;

import com.paula.pokerplanning_server.domain.model.Invite;
import com.paula.pokerplanning_server.domain.model.Participant;
import com.paula.pokerplanning_server.domain.model.ParticipantRole;
import com.paula.pokerplanning_server.domain.repository.InviteRepository;
import com.paula.pokerplanning_server.exception.BadRequestException;
import com.paula.pokerplanning_server.exception.NotFoundException;
import com.paula.pokerplanning_server.security.ActorContext;
import com.paula.pokerplanning_server.web.dto.JoinRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteRepository inviteRepository;
    private final RoomService roomService;

    @Transactional
    public String createInvite(UUID roomId, ActorContext hostActor,
                                ParticipantRole role, Instant expiresAt, Integer maxUses) {
        Participant host = roomService.requireHost(roomId, hostActor);

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256(rawToken);

        Invite invite = Invite.builder()
                .tokenHash(tokenHash)
                .roomId(roomId)
                .role(role != null ? role : ParticipantRole.PARTICIPANT)
                .expiresAt(expiresAt)
                .maxUses(maxUses)
                .creatorParticipantId(host.getId())
                .build();
        inviteRepository.save(invite);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public List<Invite> listInvites(UUID roomId, ActorContext hostActor) {
        roomService.requireHost(roomId, hostActor);
        return inviteRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
    }

    @Transactional
    public void revokeInvite(UUID inviteId, ActorContext hostActor) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new NotFoundException("Invite not found"));
        roomService.requireHost(invite.getRoomId(), hostActor);
        invite.setRevokedAt(Instant.now());
        inviteRepository.save(invite);
    }

    @Transactional(readOnly = true)
    public Invite getInviteByToken(String rawToken) {
        String hash = sha256(rawToken);
        return inviteRepository.findByTokenHash(hash)
                .orElseThrow(() -> new NotFoundException("Invite not found or invalid"));
    }

    @Transactional
    public JoinRoomResponse joinByInvite(String rawToken, ActorContext actor, String displayName) {
        String hash = sha256(rawToken);
        Invite invite = inviteRepository.findByTokenHash(hash)
                .orElseThrow(() -> new NotFoundException("Invite not found or invalid"));

        if (!invite.isValid()) {
            throw new BadRequestException("Invite is expired, revoked, or has reached its use limit");
        }

        invite.setUses(invite.getUses() + 1);
        inviteRepository.save(invite);

        return roomService.joinRoom(invite.getRoomId(), actor, displayName, invite.getRole());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
