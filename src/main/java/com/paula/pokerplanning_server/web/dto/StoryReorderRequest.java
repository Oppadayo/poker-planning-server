package com.paula.pokerplanning_server.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record StoryReorderRequest(
        @NotNull List<UUID> storyIds
) {}
