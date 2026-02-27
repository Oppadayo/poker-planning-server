package com.paula.pokerplanning_server.domain.repository;

import com.paula.pokerplanning_server.domain.model.Invite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InviteRepository extends JpaRepository<Invite, UUID> {
    Optional<Invite> findByTokenHash(String tokenHash);
    List<Invite> findByRoomIdOrderByCreatedAtDesc(UUID roomId);
}
