package com.faro.devis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Le devis d'une affaire : ses lignes et ses totaux, présentables par pièce ou par lot. */
public record DevisResponse(
        List<LigneResponse> lignes,
        List<TotalPiece> parPiece,
        List<TotalLot> parLot,
        List<TotalTva> tva,
        BigDecimal totalHt,
        BigDecimal totalTva,
        BigDecimal totalTtc) {

    public record LigneResponse(
            UUID id,
            UUID pieceId,
            UUID lotId,
            String designation,
            String description,
            String unite,
            BigDecimal quantite,
            BigDecimal prixUnitaireHt,
            BigDecimal tauxTva,
            BigDecimal montantHt,
            int ordre) {

        static LigneResponse from(LigneDevis ligne) {
            return new LigneResponse(ligne.getId(),
                ligne.getPiece() != null ? ligne.getPiece().getId() : null,
                ligne.getLot().getId(), ligne.getDesignation(), ligne.getDescription(), ligne.getUnite(),
                ligne.getQuantite(), ligne.getPrixUnitaireHt(), ligne.getTauxTva(), ligne.getMontantHt(),
                ligne.getOrdre());
        }
    }

    /** pieceId null : lignes générales. Toutes les pièces de l'affaire figurent, même sans ligne. */
    public record TotalPiece(UUID pieceId, String nom, BigDecimal totalHt) {
    }

    /** Seuls les lots utilisés par au moins une ligne figurent. */
    public record TotalLot(UUID lotId, String code, String nom, BigDecimal totalHt) {
    }

    public record TotalTva(BigDecimal taux, BigDecimal baseHt, BigDecimal montant) {
    }
}
