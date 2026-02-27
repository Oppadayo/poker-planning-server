package com.paula.pokerplanning_server.domain.repository;

import com.paula.pokerplanning_server.domain.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {
    List<Vote> findByRoundId(UUID roundId);
    Optional<Vote> findByRoundIdAndParticipantId(UUID roundId, UUID participantId);
    boolean existsByRoundIdAndParticipantId(UUID roundId, UUID participantId);

    @Modifying
    @Query("DELETE FROM Vote v WHERE v.roundId = :roundId")
    void deleteByRoundId(@Param("roundId") UUID roundId);
}
