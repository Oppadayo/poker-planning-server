package com.paula.pokerplanning_server.web.dto;

import jakarta.validation.constraints.Size;

public record StoryUpdateRequest(
        @Size(max = 200) String title,
        String description,
        @Size(max = 500) String externalRef
) {}
