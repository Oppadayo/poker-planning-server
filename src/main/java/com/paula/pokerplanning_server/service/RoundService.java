package com.paula.pokerplanning_server.service;

import com.paula.pokerplanning_server.domain.model.*;
import com.paula.pokerplanning_server.domain.repository.ParticipantRepository;
import com.paula.pokerplanning_server.domain.repository.RoundRepository;
import com.paula.pokerplanning_server.domain.repository.StoryRepository;
import com.paula.pokerplanning_server.domain.repository.VoteRepository;
import com.paula.pokerplanning_server.exception.BadRequestException;
import com.paula.pokerplanning_server.exception.ConflictException;
import com.paula.pokerplanning_server.exception.ForbiddenException;
import com.paula.pokerplanning_server.exception.NotFoundException;
import com.paula.pokerplanning_server.security.ActorContext;
import com.paula.pokerplanning_server.web.dto.RoundResponse;
import com.paula.pokerplanning_server.web.dto.VoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoundService {

    private final RoundRepository roundRepository;
    private final VoteRepository voteRepository;
    private final StoryRepository storyRepository;
    private final ParticipantRepository participantRepository;
    private final RoomService roomService;
    private final EventPublisher eventPublisher;

    // ─── Start ────────────────────────────────────────────────────────────────

    @Transactional
    public Round startRound(UUID roomId, ActorContext hostActor) {
        roomService.requireHost(roomId, hostActor);
        Room room = roomService.getActiveRoom(roomId);

        UUID storyId = room.getCurrentStoryId();
        if (storyId == null) {
            throw new BadRequestException("No story selected in room");
        }

        // Check for active round
        boolean hasActive = roundRepository.findByRoomIdAndStatusIn(roomId,
                List.of(RoundStatus.VOTING, RoundStatus.REVEALED)).isPresent();
        if (hasActive) {
            throw new ConflictException("A round is already in progress");
        }

        Round round = Round.builder()
                .roomId(roomId)
                .storyId(storyId)
                .status(RoundStatus.VOTING)
                .build();
        round = roundRepository.save(round);

        eventPublisher.publish(EventType.ROUND_STARTED, roomId, Map.of(
                "roundId", round.getId().toString(),
                "storyId", storyId.toString(),
                "status", RoundStatus.VOTING.name()
        ));

        return round;
    }

    // ─── Vote ─────────────────────────────────────────────────────────────────

    @Transactional
    public void castVote(UUID roomId, ActorContext actor, String value) {
        Participant participant = roomService.getParticipant(roomId, actor);
        if (participant.getRole() == ParticipantRole.OBSERVER) {
            throw new ForbiddenException("Observers cannot vote");
        }

        Round round = getActiveRound(roomId);
        if (round.getStatus() != RoundStatus.VOTING) {
            throw new BadRequestException("Voting is not open for this round");
        }

        // Upsert vote
        Vote vote = voteRepository.findByRoundIdAndParticipantId(round.getId(), participant.getId())
                .orElseGet(() -> Vote.builder()
                        .roundId(round.getId())
                        .participantId(participant.getId())
                        .build());
        vote.setValue(value);
        voteRepository.save(vote);

        eventPublisher.publish(EventType.VOTE_CAST, roomId, Map.of(
                "participantId", participant.getId().toString(),
                "hasVoted", true
        ));
    }

    // ─── Reveal ───────────────────────────────────────────────────────────────

    @Transactional
    public Round revealVotes(UUID roomId, ActorContext hostActor) {
        roomService.requireHost(roomId, hostActor);
        Round round = getActiveRound(roomId);

        if (round.getStatus() != RoundStatus.VOTING) {
            throw new BadRequestException("Round is not in VOTING status");
        }

        round.setStatus(RoundStatus.REVEALED);
        round.setRevealedAt(Instant.now());
        round = roundRepository.save(round);

        List<Vote> votes = voteRepository.findByRoundId(round.getId());
        List<Map<String, Object>> votePayloads = votes.stream()
                .map(v -> Map.<String, Object>of(
                        "participantId", v.getParticipantId().toString(),
                        "value", v.getValue()
                ))
                .toList();

        eventPublisher.publish(EventType.ROUND_REVEALED, roomId, Map.of(
                "roundId", round.getId().toString(),
                "votes", votePayloads
        ));

        return round;
    }

    // ─── Reset ────────────────────────────────────────────────────────────────

    @Transactional
    public Round resetRound(UUID roomId, ActorContext hostActor) {
        roomService.requireHost(roomId, hostActor);
        Round round = getActiveRound(roomId);

        voteRepository.deleteByRoundId(round.getId());
        round.setStatus(RoundStatus.VOTING);
        round.setRevealedAt(null);
        round = roundRepository.save(round);

        eventPublisher.publish(EventType.ROUND_RESET, roomId,
                Map.of("roundId", round.getId().toString()));

        return round;
    }

    // ─── Finalize ─────────────────────────────────────────────────────────────

    @Transactional
    public Round finalizeRound(UUID roomId, ActorContext hostActor, String finalEstimate) {
        roomService.requireHost(roomId, hostActor);
        Round round = getActiveRound(roomId);

        if (round.getStatus() != RoundStatus.REVEALED) {
            throw new BadRequestException("Votes must be revealed before finalizing");
        }

        round.setStatus(RoundStatus.FINALIZED);
        round.setFinalizedAt(Instant.now());
        round = roundRepository.save(round);

        // Mark story as estimated
        storyRepository.findById(round.getStoryId()).ifPresent(story -> {
            story.setStatus(StoryStatus.ESTIMATED);
            story.setFinalEstimate(finalEstimate);
            storyRepository.save(story);
        });

        eventPublisher.publish(EventType.ROUND_FINALIZED, roomId, Map.of(
                "roundId", round.getId().toString(),
                "storyId", round.getStoryId().toString(),
                "finalEstimate", finalEstimate
        ));

        return round;
    }

    // ─── Queries ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Round getActiveRound(UUID roomId) {
        return roundRepository.findByRoomIdAndStatusIn(roomId,
                        List.of(RoundStatus.VOTING, RoundStatus.REVEALED))
                .orElseThrow(() -> new NotFoundException("No active round found in room " + roomId));
    }

    @Transactional(readOnly = true)
    public RoundResponse getActiveRoundResponse(UUID roomId) {
        try {
            Round round = getActiveRound(roomId);
            return toResponse(round);
        } catch (NotFoundException e) {
            return null;
        }
    }

    public RoundResponse toResponse(Round round) {
        List<Vote> votes = voteRepository.findByRoundId(round.getId());
        boolean concealed = round.getStatus() == RoundStatus.VOTING;

        List<VoteResponse> voteResponses = votes.stream()
                .map(v -> new VoteResponse(
                        v.getParticipantId(),
                        true,
                        concealed ? null : v.getValue()
                ))
                .toList();

        // Add "not voted" entries for participants who haven't voted
        List<Participant> participants = participantRepository.findByRoomId(round.getRoomId());
        List<UUID> votedIds = votes.stream().map(Vote::getParticipantId).toList();

        List<VoteResponse> notVoted = participants.stream()
                .filter(p -> p.getRole() != ParticipantRole.OBSERVER)
                .filter(p -> !votedIds.contains(p.getId()))
                .map(p -> new VoteResponse(p.getId(), false, null))
                .toList();

        List<VoteResponse> allVotes = new java.util.ArrayList<>(voteResponses);
        allVotes.addAll(notVoted);

        return RoundResponse.from(round, allVotes);
    }
}
