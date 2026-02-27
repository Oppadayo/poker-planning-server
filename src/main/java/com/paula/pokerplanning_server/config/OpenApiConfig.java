package com.paula.pokerplanning_server.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration.
 *
 * Three security schemes are available:
 *
 *  1. bearerAuth     → JWT do usuário autenticado (POST /auth/login)
 *                      Header: Authorization: Bearer <token>
 *
 *  2. guestToken     → Token assinado de convidado HOST
 *                      Obrigatório em operações exclusivas do host quando o ator é guest.
 *                      Header: X-Guest-Token: <token>
 *
 *  3. guestId        → Identificador do convidado (UUID gerado no frontend)
 *                      Necessário em operações abertas de guests (votar, entrar na sala, etc.)
 *                      Header: X-Guest-Id: <uuid>
 *
 * Como usar no Swagger UI:
 *   1. Crie uma sala  → POST /rooms  (envie X-Guest-Id; retorna guestToken no body)
 *   2. Clique em "Authorize" e cole o guestToken recebido no campo "guestToken"
 *   3. Para usuários: POST /auth/login → copie o token → cole em "bearerAuth"
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Poker Planning API",
                version = "1.0.0",
                description = """
                        API REST + WebSocket para sessões de Planning Poker.

                        **Autenticação:**
                        - **Usuário registrado:** faça login em `POST /auth/login`, obtenha o JWT e use em "Authorize → bearerAuth"
                        - **Convidado (guest):** gere um UUID no frontend e passe sempre em `X-Guest-Id`.
                          Operações de host exigem também o `X-Guest-Token` retornado ao criar/entrar na sala.

                        **WebSocket:**
                        Conecte-se em `/ws` (SockJS) e envie votos para `/app/rooms/{roomId}/vote`.
                        Ouça eventos em `/topic/rooms/{roomId}/events`.
                        """,
                contact = @Contact(name = "Poker Planning Team"),
                license = @License(name = "MIT")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local")
        }
)
@SecuritySchemes({
        @SecurityScheme(
                name = "bearerAuth",
                type = SecuritySchemeType.HTTP,
                scheme = "bearer",
                bearerFormat = "JWT",
                description = "JWT para usuários autenticados. Obtenha via POST /auth/login e cole aqui."
        ),
        @SecurityScheme(
                name = "guestToken",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER,
                paramName = "X-Guest-Token",
                description = "Token assinado de convidado (retornado ao criar/entrar na sala). Necessário em operações host-only para guests."
        ),
        @SecurityScheme(
                name = "guestId",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER,
                paramName = "X-Guest-Id",
                description = "UUID do guest gerado no frontend (ex: crypto.randomUUID()). Necessário para operações regulares de guests."
        )
})
public class OpenApiConfig {

    /**
     * Adiciona os três esquemas de segurança globalmente para que o botão
     * "Authorize" do Swagger UI exiba todos de uma vez.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .addSecurityItem(new SecurityRequirement().addList("guestToken"))
                .addSecurityItem(new SecurityRequirement().addList("guestId"));
    }
}
