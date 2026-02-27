package com.paula.pokerplanning_server.domain.repository;

import com.paula.pokerplanning_server.domain.model.Story;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoryRepository extends JpaRepository<Story, UUID> {
    List<Story> findByRoomIdOrderByOrderIndexAsc(UUID roomId);
    int countByRoomId(UUID roomId);
}
