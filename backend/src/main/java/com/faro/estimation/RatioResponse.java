package com.faro.estimation;

import java.math.BigDecimal;
import java.util.UUID;

import com.faro.affaire.NiveauTravaux;
import com.faro.affaire.TypePiece;

/** Le ratio et, pour le recaler, ce qui est constaté dans les devis de l'entreprise (null si aucun). */
public record RatioResponse(
        UUID id,
        TypePiece typePiece,
        NiveauTravaux niveauTravaux,
        BigDecimal prixMinM2,
        BigDecimal prixMaxM2,
        Constate constate) {

    /** €/m² HT constatés sur les pièces devisées de ce type et de ce niveau. */
    public record Constate(int pieces, BigDecimal minM2, BigDecimal medianeM2, BigDecimal maxM2) {
    }
}
