package com.paula.pokerplanning_server.web.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String username,
        String email
) {}
