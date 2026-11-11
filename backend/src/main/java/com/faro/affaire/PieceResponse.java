package com.faro.affaire;

import java.math.BigDecimal;
import java.util.UUID;

/** surfaceMursM2 est calculée (périmètre × hauteur) ; le plafond a la surface du sol. */
public record PieceResponse(UUID id, String nom, BigDecimal surfaceM2, BigDecimal perimetreMl, BigDecimal hauteurM,
        BigDecimal surfaceMursM2, int ordre) {

    public static PieceResponse from(Piece piece) {
        return new PieceResponse(piece.getId(), piece.getNom(), piece.getSurfaceM2(), piece.getPerimetreMl(),
            piece.getHauteurM(), piece.getSurfaceMursM2(), piece.getOrdre());
    }
}
