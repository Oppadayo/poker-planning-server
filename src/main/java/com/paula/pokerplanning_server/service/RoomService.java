package com.paula.pokerplanning_server.service;

import com.paula.pokerplanning_server.domain.model.*;
import com.paula.pokerplanning_server.domain.repository.ParticipantRepository;
import com.paula.pokerplanning_server.domain.repository.RoomRepository;
import com.paula.pokerplanning_server.exception.BadRequestException;
import com.paula.pokerplanning_server.exception.ConflictException;
import com.paula.pokerplanning_server.exception.ForbiddenException;
import com.paula.pokerplanning_server.exception.NotFoundException;
import com.paula.pokerplanning_server.security.ActorContext;
import com.paula.pokerplanning_server.security.GuestTokenProvider;
import com.paula.pokerplanning_server.web.dto.JoinRoomResponse;
import com.paula.pokerplanning_server.web.dto.ParticipantResponse;
import com.paula.pokerplanning_server.web.dto.RoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final GuestTokenProvider guestTokenProvider;
    private final EventPublisher eventPublisher;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    // ─── Create ──────────────────────────────────────────────────────────────

    @Transactional
    public JoinRoomResponse createRoom(ActorContext actor, String displayName,
                                       DeckType deckType, boolean allowObservers, String roomName) {
        String code = generateUniqueCode();

        Room room = Room.builder()
                .name(roomName)
                .code(code)
                .creatorUserId(actor.isUser() ? actor.getUserId() : null)
                .creatorGuestId(actor.isGuest() ? actor.getGuestId() : null)
                .settings(RoomSettings.builder()
                        .deckType(deckType != null ? deckType : DeckType.FIBONACCI)
                        .allowObservers(allowObservers)
                        .build())
                .build();
        room = roomRepository.save(room);

        Participant host = Participant.builder()
                .roomId(room.getId())
                .userId(actor.isUser() ? actor.getUserId() : null)
                .guestId(actor.isGuest() ? actor.getGuestId() : null)
                .role(ParticipantRole.HOST)
                .displayName(displayName)
                .online(true)
                .build();
        host = participantRepository.save(host);

        String guestToken = null;
        if (actor.isGuest()) {
            guestToken = guestTokenProvider.generateToken(
                    actor.getGuestId(), host.getId(), room.getId(), ParticipantRole.HOST);
        }

        eventPublisher.publish(EventType.PARTICIPANT_JOINED, room.getId(), Map.of(
                "participantId", host.getId().toString(),
                "displayName", host.getDisplayName(),
                "role", host.getRole().name()
        ));

        log.info("Room {} created by actor {}", room.getId(), actor.isUser() ? actor.getUserId() : actor.getGuestId());

        return new JoinRoomResponse(RoomResponse.from(room), ParticipantResponse.from(host), guestToken);
    }

    // ─── Join ─────────────────────────────────────────────────────────────────

    @Transactional
    public JoinRoomResponse joinRoom(UUID roomId, ActorContext actor,
                                     String displayName, ParticipantRole requestedRole) {
        Room room = getActiveRoom(roomId);

        ParticipantRole role = resolveRole(room, requestedRole);

        // Check if already a participant
        Participant participant = findExistingParticipant(roomId, actor);
        if (participant != null) {
            participant.setDisplayName(displayName);
            participant.setOnline(true);
            participant = participantRepository.save(participant);
        } else {
            participant = Participant.builder()
                    .roomId(roomId)
                    .userId(actor.isUser() ? actor.getUserId() : null)
                    .guestId(actor.isGuest() ? actor.getGuestId() : null)
                    .role(role)
                    .displayName(displayName)
                    .online(true)
                    .build();
            participant = participantRepository.save(participant);
        }

        String guestToken = null;
        if (actor.isGuest()) {
            guestToken = guestTokenProvider.generateToken(
                    actor.getGuestId(), participant.getId(), roomId, participant.getRole());
        }

        final UUID participantId = participant.getId();
        final String finalDisplayName = participant.getDisplayName();
        final ParticipantRole finalRole = participant.getRole();
        eventPublisher.publish(EventType.PARTICIPANT_JOINED, roomId, Map.of(
                "participantId", participantId.toString(),
                "displayName", finalDisplayName,
                "role", finalRole.name()
        ));

        return new JoinRoomResponse(RoomResponse.from(room), ParticipantResponse.from(participant), guestToken);
    }

    @Transactional
    public JoinRoomResponse joinByCode(String code, ActorContext actor,
                                       String displayName, ParticipantRole requestedRole) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Room not found with code: " + code));
        return joinRoom(room.getId(), actor, displayName, requestedRole);
    }

    // ─── Leave ────────────────────────────────────────────────────────────────

    @Transactional
    public void leaveRoom(UUID roomId, ActorContext actor) {
        Participant participant = getParticipant(roomId, actor);
        participant.setOnline(false);
        participantRepository.save(participant);

        eventPublisher.publish(EventType.PARTICIPANT_LEFT, roomId,
                Map.of("participantId", participant.getId().toString()));
    }

    // ─── Host actions ─────────────────────────────────────────────────────────

    @Transactional
    public void kickParticipant(UUID roomId, UUID targetParticipantId, ActorContext hostActor) {
        requireHost(roomId, hostActor);
        Participant target = participantRepository.findById(targetParticipantId)
                .orElseThrow(() -> new NotFoundException("Participant not found"));
        if (!target.getRoomId().equals(roomId)) {
            throw new BadRequestException("Participant not in this room");
        }
        participantRepository.delete(target);

        eventPublisher.publish(EventType.PARTICIPANT_KICKED, roomId,
                Map.of("participantId", targetParticipantId.toString()));
    }

    @Transactional
    public void transferHost(UUID roomId, UUID newHostParticipantId, ActorContext hostActor) {
        Participant currentHost = requireHost(roomId, hostActor);
        Participant newHost = participantRepository.findById(newHostParticipantId)
                .orElseThrow(() -> new NotFoundException("Participant not found"));
        if (!newHost.getRoomId().equals(roomId)) {
            throw new BadRequestException("Participant not in this room");
        }

        currentHost.setRole(ParticipantRole.PARTICIPANT);
        newHost.setRole(ParticipantRole.HOST);
        participantRepository.save(currentHost);
        participantRepository.save(newHost);

        eventPublisher.publish(EventType.HOST_TRANSFERRED, roomId, Map.of(
                "newHostParticipantId", newHostParticipantId.toString(),
                "previousHostParticipantId", currentHost.getId().toString()
        ));
    }

    @Transactional
    public void closeRoom(UUID roomId, ActorContext hostActor) {
        requireHost(roomId, hostActor);
        Room room = getActiveRoom(roomId);
        room.setStatus(RoomStatus.CLOSED);
        roomRepository.save(room);

        eventPublisher.publish(EventType.ROOM_CLOSED, roomId, Map.of());
    }

    // ─── Queries ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Room getActiveRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room not found: " + roomId));
        if (room.getStatus() == RoomStatus.CLOSED) {
            throw new BadRequestException("Room is closed");
        }
        return room;
    }

    @Transactional(readOnly = true)
    public Room getRoom(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room not found: " + roomId));
    }

    @Transactional(readOnly = true)
    public List<Participant> getParticipants(UUID roomId) {
        return participantRepository.findByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public Participant getParticipant(UUID roomId, ActorContext actor) {
        Participant p = findExistingParticipant(roomId, actor);
        if (p == null) {
            throw new ForbiddenException("Not a participant in room " + roomId);
        }
        return p;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Participant findExistingParticipant(UUID roomId, ActorContext actor) {
        if (actor.isUser()) {
            return participantRepository.findByRoomIdAndUserId(roomId, actor.getUserId()).orElse(null);
        } else {
            return participantRepository.findByRoomIdAndGuestId(roomId, actor.getGuestId()).orElse(null);
        }
    }

    public Participant requireHost(UUID roomId, ActorContext hostActor) {
        Participant host;
        if (hostActor.hasGuestToken()) {
            // Token already validated and contains role=HOST
            host = participantRepository.findById(hostActor.getGuestParticipantId())
                    .orElseThrow(() -> new ForbiddenException("Participant not found"));
            if (host.getRole() != ParticipantRole.HOST) {
                throw new ForbiddenException("Only host can perform this action");
            }
        } else {
            host = getParticipant(roomId, hostActor);
            if (host.getRole() != ParticipantRole.HOST) {
                throw new ForbiddenException("Only host can perform this action");
            }
        }
        return host;
    }

    private ParticipantRole resolveRole(Room room, ParticipantRole requested) {
        if (requested == ParticipantRole.OBSERVER && !room.getSettings().isAllowObservers()) {
            throw new BadRequestException("This room does not allow observers");
        }
        return requested != null ? requested : ParticipantRole.PARTICIPANT;
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            code = generateCode();
            attempts++;
            if (attempts > 20) {
                throw new RuntimeException("Failed to generate unique room code");
            }
        } while (roomRepository.existsByCode(code));
        return code;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
