package com.faro.devis;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Le devis d'une affaire : ses lignes et ses totaux, présentables par pièce ou par lot. */
public record DevisResponse(
        List<LigneResponse> lignes,
        List<TotalPiece> parPiece,
        List<TotalLot> parLot,
        List<TotalTva> tva,
        BigDecimal totalHt,
        BigDecimal totalTva,
        BigDecimal totalTtc,
        /** Total HT si chaque gamme était retenue partout ; null sans ligne d'option. */
        Map<Gamme, BigDecimal> totauxParGamme) {

    public record LigneResponse(
            UUID id,
            UUID pieceId,
            UUID lotId,
            UUID ouvrageId,
            String designation,
            String description,
            String unite,
            BigDecimal quantite,
            BigDecimal prixUnitaireHt,
            BigDecimal tauxTva,
            BigDecimal montantHt,
            Gamme gamme,
            BigDecimal debourseUnitaireHt,
            /** false : ligne d'une gamme non retenue, hors totaux. */
            boolean incluse,
            int ordre) {

        public static LigneResponse from(LigneDevis ligne) {
            return new LigneResponse(ligne.getId(),
                ligne.getPiece() != null ? ligne.getPiece().getId() : null,
                ligne.getLot().getId(), ligne.getOuvrage() != null ? ligne.getOuvrage().getId() : null,
                ligne.getDesignation(), ligne.getDescription(), ligne.getUnite(),
                ligne.getQuantite(), ligne.getPrixUnitaireHt(), ligne.getTauxTva(), ligne.getMontantHt(),
                ligne.getGamme(), ligne.getDebourseUnitaireHt(), ligne.isIncluse(), ligne.getOrdre());
        }
    }

    /**
     * pieceId null : lignes générales. Toutes les pièces de l'affaire figurent, même sans ligne.
     * gammes : total HT de la pièce pour chaque gamme (null si la pièce n'a pas de ligne d'option).
     */
    public record TotalPiece(UUID pieceId, String nom, BigDecimal totalHt, Gamme gammeChoisie,
            Map<Gamme, BigDecimal> gammes) {
    }

    /** Seuls les lots utilisés par au moins une ligne figurent. */
    public record TotalLot(UUID lotId, String code, String nom, BigDecimal totalHt) {
    }

    public record TotalTva(BigDecimal taux, BigDecimal baseHt, BigDecimal montant) {
    }
}
