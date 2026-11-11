package com.faro.marge;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Marge par lot et totale. vendu = lignes retenues du devis + avenants validés.
 * Prévu = quantité × déboursé sec ; lignesSansDebourse indique les lignes vendues sans coût prévu
 * (la marge prévue est alors surestimée). Réel = dépenses saisies.
 */
public record MargeResponse(List<Lot> lots, Total total, List<DepenseResponse> depenses) {

    public record Lot(UUID lotId, String code, String nom, BigDecimal venduHt, BigDecimal coutPrevuHt,
            BigDecimal coutReelHt, BigDecimal margePrevueHt, BigDecimal margeReelleHt, int lignesSansDebourse) {
    }

    public record Total(BigDecimal venduHt, BigDecimal coutPrevuHt, BigDecimal coutReelHt, BigDecimal margePrevueHt,
            BigDecimal margeReelleHt, BigDecimal tauxMargePrevue, BigDecimal tauxMargeReelle, int lignesSansDebourse) {
    }
}
