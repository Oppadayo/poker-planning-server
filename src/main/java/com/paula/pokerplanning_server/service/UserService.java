package com.paula.pokerplanning_server.service;

import com.paula.pokerplanning_server.domain.model.Participant;
import com.paula.pokerplanning_server.domain.model.Room;
import com.paula.pokerplanning_server.domain.model.RoomStatus;
import com.paula.pokerplanning_server.domain.model.User;
import com.paula.pokerplanning_server.domain.repository.ParticipantRepository;
import com.paula.pokerplanning_server.domain.repository.RoomRepository;
import com.paula.pokerplanning_server.domain.repository.UserRepository;
import com.paula.pokerplanning_server.exception.BadRequestException;
import com.paula.pokerplanning_server.exception.ConflictException;
import com.paula.pokerplanning_server.exception.ForbiddenException;
import com.paula.pokerplanning_server.exception.NotFoundException;
import com.paula.pokerplanning_server.security.GuestTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final GuestTokenProvider guestTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already taken: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered: " + email);
        }
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User authenticate(String usernameOrEmail, String password) {
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Invalid credentials");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User getById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    /**
     * Claims rooms and participant slots created as a guest, associating them with the authenticated user.
     */
    @Transactional
    public void claimSessions(UUID userId, String guestId, String guestToken) {
        // Validate the guest token
        GuestTokenProvider.GuestClaims claims = guestTokenProvider.validate(guestToken);
        if (!claims.guestId().equals(guestId)) {
            throw new ForbiddenException("Guest ID does not match token");
        }

        // Re-associate rooms created by this guestId
        List<Room> guestRooms = roomRepository.findByCreatorGuestId(guestId);
        for (Room room : guestRooms) {
            room.setCreatorUserId(userId);
            room.setCreatorGuestId(null);
            roomRepository.save(room);
        }

        // Re-associate participant records
        List<Participant> guestParticipants = participantRepository.findByRoomId(claims.roomId())
                .stream()
                .filter(p -> guestId.equals(p.getGuestId()))
                .toList();

        for (Participant p : guestParticipants) {
            if (!participantRepository.existsByRoomIdAndUserId(p.getRoomId(), userId)) {
                p.setUserId(userId);
                p.setGuestId(null);
                participantRepository.save(p);
            }
        }

        log.info("Claimed sessions for guest {} -> user {}", guestId, userId);
    }

    @Transactional(readOnly = true)
    public List<Room> getSessionsByUser(UUID userId) {
        return roomRepository.findByCreatorUserIdAndStatus(userId, RoomStatus.ACTIVE);
    }
}
