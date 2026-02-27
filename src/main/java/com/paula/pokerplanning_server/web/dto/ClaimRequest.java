package com.paula.pokerplanning_server.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ClaimRequest(
        @NotBlank String guestId,
        @NotBlank String guestToken
) {}
