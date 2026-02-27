package com.paula.pokerplanning_server.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSettings {

    @Enumerated(EnumType.STRING)
    @Column(name = "deck_type", nullable = false, length = 30)
    @Builder.Default
    private DeckType deckType = DeckType.FIBONACCI;

    @Column(name = "allow_observers", nullable = false)
    @Builder.Default
    private boolean allowObservers = true;
}
