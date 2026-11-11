package com.faro.avenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Décision du client sur un avenant envoyé : qui a décidé (nom du client) et éventuel commentaire. */
public record DecisionRequest(
        @NotNull StatutAvenant decision,
        @NotBlank @Size(max = 255) String par,
        @Size(max = 5000) String commentaire) {
}
