package com.faro.affaire;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ordre facultatif : à la création, la pièce est placée en dernier. */
public record PieceRequest(
        @NotBlank @Size(max = 255) String nom,
        @DecimalMin("0") @Digits(integer = 6, fraction = 2) BigDecimal surfaceM2,
        Integer ordre) {
}
