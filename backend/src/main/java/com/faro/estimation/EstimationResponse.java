package com.faro.estimation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.faro.affaire.NiveauTravaux;
import com.faro.affaire.TypePiece;

/**
 * Estimation rapide HT : surface × ratio de chaque pièce. Une pièce sans surface, type ou niveau
 * n'est pas comptée (manque indique ce qu'il faut saisir). devisHt permet de comparer avec le devis détaillé.
 */
public record EstimationResponse(
        List<PieceEstimee> pieces,
        BigDecimal totalMinHt,
        BigDecimal totalMaxHt,
        BigDecimal devisHt) {

    public record PieceEstimee(
            UUID pieceId,
            String nom,
            TypePiece typePiece,
            NiveauTravaux niveauTravaux,
            BigDecimal surfaceM2,
            BigDecimal minHt,
            BigDecimal maxHt,
            String manque) {
    }
}
