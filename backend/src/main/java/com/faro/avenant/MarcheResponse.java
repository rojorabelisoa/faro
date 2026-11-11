package com.faro.avenant;

import java.math.BigDecimal;

/**
 * Montant du marché : dossier travaux (version de référence du chantier) + avenants validés.
 * Avant le chantier (chantier = false), le montant initial est celui du devis courant.
 */
public record MarcheResponse(
        boolean chantier,
        BigDecimal initialHt,
        BigDecimal initialTtc,
        BigDecimal avenantsValidesHt,
        BigDecimal avenantsValidesTtc,
        BigDecimal actuelHt,
        BigDecimal actuelTtc,
        BigDecimal avenantsEnAttenteHt) {
}
