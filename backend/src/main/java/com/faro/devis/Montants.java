package com.faro.devis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/** Règles de calcul des montants (arrondi au centime, demi supérieur). */
public final class Montants {

    /** Taux de TVA applicables en rénovation : 5,5 % (rénovation énergétique), 10 %, 20 %, 0 % (autoliquidation). */
    public static final Set<BigDecimal> TAUX_TVA = Set.of(
        new BigDecimal("0.00"), new BigDecimal("5.50"), new BigDecimal("10.00"), new BigDecimal("20.00"));

    public static final BigDecimal TAUX_TVA_PAR_DEFAUT = new BigDecimal("10.00");

    private static final BigDecimal CENT = new BigDecimal("100");

    private Montants() {
    }

    public static BigDecimal montantHt(BigDecimal quantite, BigDecimal prixUnitaireHt) {
        return arrondi(quantite.multiply(prixUnitaireHt));
    }

    /** TVA calculée sur la base HT totale d'un taux (et non ligne par ligne), comme sur une facture. */
    public static BigDecimal tva(BigDecimal baseHt, BigDecimal taux) {
        return arrondi(baseHt.multiply(taux).divide(CENT));
    }

    public static boolean tauxValide(BigDecimal taux) {
        return TAUX_TVA.contains(taux.setScale(2, RoundingMode.HALF_UP));
    }

    public static BigDecimal arrondi(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP);
    }
}
