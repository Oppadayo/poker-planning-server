package com.paula.pokerplanning_server.web.rest;

import com.paula.pokerplanning_server.domain.model.User;
import com.paula.pokerplanning_server.security.JwtTokenProvider;
import com.paula.pokerplanning_server.service.UserService;
import com.paula.pokerplanning_server.web.dto.AuthLoginRequest;
import com.paula.pokerplanning_server.web.dto.AuthRegisterRequest;
import com.paula.pokerplanning_server.web.dto.AuthResponse;
import com.paula.pokerplanning_server.web.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Registro e login de usuários. Retorna um JWT para uso no header Authorization: Bearer.")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Registrar usuário", description = "Cria uma nova conta de usuário e retorna o JWT de acesso.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "409", description = "Username ou e-mail já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody @Valid AuthRegisterRequest request) {
        User user = userService.register(request.username(), request.email(), request.password());
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }

    @Operation(summary = "Login", description = "Autentica com username/e-mail + senha e retorna o JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login bem-sucedido"),
            @ApiResponse(responseCode = "400", description = "Credenciais inválidas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthLoginRequest request) {
        User user = userService.authenticate(request.usernameOrEmail(), request.password());
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }
}
