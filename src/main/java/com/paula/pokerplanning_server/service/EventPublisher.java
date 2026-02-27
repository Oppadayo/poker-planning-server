package com.paula.pokerplanning_server.service;

import tools.jackson.databind.ObjectMapper;
import com.paula.pokerplanning_server.domain.model.EventType;
import com.paula.pokerplanning_server.event.RoomEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHANNEL_PREFIX = "rooms:";

    public void publish(EventType type, UUID roomId, Map<String, Object> payload) {
        RoomEvent event = RoomEvent.of(type, roomId, payload);
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL_PREFIX + roomId, json);
            log.debug("Published event {} for room {}", type, roomId);
        } catch (Exception e) {
            log.error("Failed to publish event {} for room {}: {}", type, roomId, e.getMessage(), e);
        }
    }
}
