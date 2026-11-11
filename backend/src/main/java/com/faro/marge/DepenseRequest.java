package com.faro.marge;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Montant négatif autorisé (avoir fournisseur). Date du jour si absente. */
public record DepenseRequest(
        @NotNull UUID lotId,
        @NotNull NatureDepense nature,
        @NotBlank @Size(max = 500) String libelle,
        @Size(max = 255) String fournisseur,
        @NotNull @Digits(integer = 10, fraction = 2) BigDecimal montantHt,
        LocalDate dateDepense) {
}
