package com.faro.investisseur;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class BilanDtos {

    private BilanDtos() {
    }

    public record BilanRequest(
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal prixAchat,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal fraisAcquisition,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal autresFrais,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal prixRevente,
            @NotNull @Min(1) @Max(240) Integer dureeMois) {
    }

    /** Indicateurs d'un scénario : coûts TTC, marge, rentabilité sur coût total, TRI annualisé (en %). */
    public record Indicateurs(String scenario, BigDecimal travauxTtc, BigDecimal coutTotal, BigDecimal margeBrute,
            BigDecimal rentabilite, BigDecimal triAnnuel, int dureeMois) {
    }

    /** hypotheses null tant que le bilan n'a pas été saisi. */
    public record BilanResponse(BilanRequest hypotheses, BigDecimal travauxTtc, List<Indicateurs> scenarios) {
    }
}
