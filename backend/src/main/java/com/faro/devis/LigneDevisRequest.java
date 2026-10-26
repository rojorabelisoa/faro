package com.faro.devis;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * pieceId null = ligne générale. tauxTva null = taux par défaut (10 %).
 * Ordre facultatif : à la création, la ligne est placée en dernier.
 */
public record LigneDevisRequest(
        UUID pieceId,
        @NotNull UUID lotId,
        @NotBlank @Size(max = 500) String designation,
        String description,
        @NotBlank @Size(max = 20) String unite,
        @NotNull @DecimalMin("0") @Digits(integer = 9, fraction = 3) BigDecimal quantite,
        @NotNull @Digits(integer = 10, fraction = 2) BigDecimal prixUnitaireHt,
        BigDecimal tauxTva,
        Integer ordre) {
}
