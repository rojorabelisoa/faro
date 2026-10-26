package com.faro.affaire;

import java.math.BigDecimal;
import java.util.UUID;

public record PieceResponse(UUID id, String nom, BigDecimal surfaceM2, int ordre) {

    public static PieceResponse from(Piece piece) {
        return new PieceResponse(piece.getId(), piece.getNom(), piece.getSurfaceM2(), piece.getOrdre());
    }
}
