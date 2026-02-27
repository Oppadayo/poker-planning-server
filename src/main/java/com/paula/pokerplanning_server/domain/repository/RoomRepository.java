package com.paula.pokerplanning_server.domain.repository;

import com.paula.pokerplanning_server.domain.model.Room;
import com.paula.pokerplanning_server.domain.model.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByCode(String code);
    boolean existsByCode(String code);
    List<Room> findByCreatorUserIdAndStatus(UUID userId, RoomStatus status);
    List<Room> findByCreatorGuestId(String guestId);
}
