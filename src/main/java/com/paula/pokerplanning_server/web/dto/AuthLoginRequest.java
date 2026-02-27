package com.paula.pokerplanning_server.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
        @NotBlank String usernameOrEmail,
        @NotBlank String password
) {}
