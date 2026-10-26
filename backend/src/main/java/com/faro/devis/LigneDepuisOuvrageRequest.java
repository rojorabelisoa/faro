package com.faro.devis;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** pieceId null = ligne générale. */
public record LigneDepuisOuvrageRequest(
        @NotNull UUID ouvrageId,
        UUID pieceId,
        @NotNull @DecimalMin("0") @Digits(integer = 9, fraction = 3) BigDecimal quantite) {
}
