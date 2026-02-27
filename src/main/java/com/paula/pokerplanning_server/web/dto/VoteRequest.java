package com.paula.pokerplanning_server.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VoteRequest(
        @NotBlank String value
) {}
