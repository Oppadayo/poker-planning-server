package com.paula.pokerplanning_server.domain.repository;

import com.paula.pokerplanning_server.domain.model.Participant;
import com.paula.pokerplanning_server.domain.model.ParticipantRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    Optional<Participant> findByRoomIdAndUserId(UUID roomId, UUID userId);
    Optional<Participant> findByRoomIdAndGuestId(UUID roomId, String guestId);
    List<Participant> findByRoomId(UUID roomId);
    List<Participant> findByRoomIdAndOnlineTrue(UUID roomId);
    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);
    boolean existsByRoomIdAndGuestId(UUID roomId, String guestId);
    Optional<Participant> findByRoomIdAndRole(UUID roomId, ParticipantRole role);

    @Modifying
    @Query("UPDATE Participant p SET p.online = false WHERE p.roomId = :roomId")
    void setAllOfflineInRoom(@Param("roomId") UUID roomId);

    @Query("SELECT p FROM Participant p WHERE p.userId = :userId")
    List<Participant> findByUserId(@Param("userId") UUID userId);
}
