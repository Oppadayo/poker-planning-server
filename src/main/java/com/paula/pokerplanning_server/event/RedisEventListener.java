package com.paula.pokerplanning_server.event;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Called by Redis MessageListenerAdapter when a message arrives on a rooms channel.
     * The message is the JSON-serialized RoomEvent.
     */
    public void handleMessage(String message) {
        try {
            RoomEvent event = objectMapper.readValue(message, RoomEvent.class);
            String topic = "/topic/rooms/" + event.roomId() + "/events";
            messagingTemplate.convertAndSend(topic, event);
            log.debug("Broadcast event {} to {}", event.type(), topic);
        } catch (Exception e) {
            log.error("Failed to process Redis event: {}", e.getMessage(), e);
        }
    }
}
