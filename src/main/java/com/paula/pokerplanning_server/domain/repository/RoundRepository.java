package com.paula.pokerplanning_server.domain.repository;

import com.paula.pokerplanning_server.domain.model.Round;
import com.paula.pokerplanning_server.domain.model.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundRepository extends JpaRepository<Round, UUID> {
    Optional<Round> findByRoomIdAndStatusIn(UUID roomId, List<RoundStatus> statuses);
    List<Round> findByStoryIdOrderByStartedAtAsc(UUID storyId);
}
