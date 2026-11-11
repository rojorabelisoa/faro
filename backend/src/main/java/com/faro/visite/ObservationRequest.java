package com.faro.visite;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** pieceId null = observation générale sur l'affaire. */
public record ObservationRequest(UUID pieceId, @NotBlank @Size(max = 20000) String texte) {
}
