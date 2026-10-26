package com.faro.bibliotheque;

import java.math.BigDecimal;
import java.util.UUID;

public record OuvrageResponse(
        UUID id,
        UUID lotId,
        String designation,
        String description,
        String unite,
        BigDecimal prixUnitaireHt,
        BigDecimal tauxTva) {

    public static OuvrageResponse from(Ouvrage ouvrage) {
        return new OuvrageResponse(ouvrage.getId(), ouvrage.getLot().getId(), ouvrage.getDesignation(),
            ouvrage.getDescription(), ouvrage.getUnite(), ouvrage.getPrixUnitaireHt(), ouvrage.getTauxTva());
    }
}
