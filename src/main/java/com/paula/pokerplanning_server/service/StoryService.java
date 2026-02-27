package com.paula.pokerplanning_server.service;

import com.paula.pokerplanning_server.domain.model.EventType;
import com.paula.pokerplanning_server.domain.model.Story;
import com.paula.pokerplanning_server.domain.model.StoryStatus;
import com.paula.pokerplanning_server.domain.repository.RoomRepository;
import com.paula.pokerplanning_server.domain.repository.StoryRepository;
import com.paula.pokerplanning_server.exception.BadRequestException;
import com.paula.pokerplanning_server.exception.NotFoundException;
import com.paula.pokerplanning_server.security.ActorContext;
import com.paula.pokerplanning_server.web.dto.StoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final EventPublisher eventPublisher;

    @Transactional
    public Story createStory(UUID roomId, ActorContext hostActor,
                              String title, String description, String externalRef) {
        roomService.requireHost(roomId, hostActor);
        ensureRoomExists(roomId);

        int maxOrder = storyRepository.countByRoomId(roomId);
        Story story = Story.builder()
                .roomId(roomId)
                .title(title)
                .description(description)
                .externalRef(externalRef)
                .orderIndex(maxOrder)
                .build();
        story = storyRepository.save(story);

        eventPublisher.publish(EventType.STORY_CREATED, roomId,
                Map.of("story", StoryResponse.from(story)));

        return story;
    }

    @Transactional
    public Story updateStory(UUID storyId, ActorContext hostActor,
                              String title, String description, String externalRef) {
        Story story = getStory(storyId);
        roomService.requireHost(story.getRoomId(), hostActor);

        if (title != null) story.setTitle(title);
        if (description != null) story.setDescription(description);
        if (externalRef != null) story.setExternalRef(externalRef);
        story = storyRepository.save(story);

        eventPublisher.publish(EventType.STORY_UPDATED, story.getRoomId(),
                Map.of("story", StoryResponse.from(story)));

        return story;
    }

    @Transactional
    public void deleteStory(UUID storyId, ActorContext hostActor) {
        Story story = getStory(storyId);
        roomService.requireHost(story.getRoomId(), hostActor);
        storyRepository.delete(story);

        eventPublisher.publish(EventType.STORY_DELETED, story.getRoomId(),
                Map.of("storyId", storyId.toString()));
    }

    @Transactional
    public List<Story> reorderStories(UUID roomId, List<UUID> orderedIds, ActorContext hostActor) {
        roomService.requireHost(roomId, hostActor);
        List<Story> stories = storyRepository.findByRoomIdOrderByOrderIndexAsc(roomId);

        for (int i = 0; i < orderedIds.size(); i++) {
            final int index = i;
            final UUID id = orderedIds.get(i);
            stories.stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst()
                    .ifPresent(s -> s.setOrderIndex(index));
        }
        stories = storyRepository.saveAll(stories);

        eventPublisher.publish(EventType.STORY_REORDERED, roomId,
                Map.of("storyIds", orderedIds.stream().map(UUID::toString).toList()));

        return stories;
    }

    @Transactional
    public Story selectCurrentStory(UUID roomId, UUID storyId, ActorContext hostActor) {
        roomService.requireHost(roomId, hostActor);
        Story story = getStory(storyId);
        if (!story.getRoomId().equals(roomId)) {
            throw new BadRequestException("Story does not belong to this room");
        }

        var room = roomService.getActiveRoom(roomId);
        // Clear previous selection
        storyRepository.findByRoomIdOrderByOrderIndexAsc(roomId).stream()
                .filter(s -> s.getStatus() == StoryStatus.SELECTED)
                .forEach(s -> {
                    s.setStatus(StoryStatus.PENDING);
                    storyRepository.save(s);
                });

        story.setStatus(StoryStatus.SELECTED);
        story = storyRepository.save(story);

        room.setCurrentStoryId(storyId);
        roomRepository.save(room);

        eventPublisher.publish(EventType.STORY_SELECTED, roomId,
                Map.of("storyId", storyId.toString()));

        return story;
    }

    @Transactional(readOnly = true)
    public List<Story> getStoriesByRoom(UUID roomId) {
        return storyRepository.findByRoomIdOrderByOrderIndexAsc(roomId);
    }

    @Transactional(readOnly = true)
    public Story getStory(UUID storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found: " + storyId));
    }

    private void ensureRoomExists(UUID roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new NotFoundException("Room not found: " + roomId);
        }
    }
}
