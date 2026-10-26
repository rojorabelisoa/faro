package com.faro.bibliotheque;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** tauxTva null = taux par défaut (10 %). */
public record OuvrageRequest(
        @NotNull UUID lotId,
        @NotBlank @Size(max = 500) String designation,
        String description,
        @NotBlank @Size(max = 20) String unite,
        @NotNull @Digits(integer = 10, fraction = 2) BigDecimal prixUnitaireHt,
        BigDecimal tauxTva) {
}
