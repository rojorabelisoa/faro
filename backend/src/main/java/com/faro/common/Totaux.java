package com.faro.common;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/** Totaux HT / TVA par taux / TTC d'un ensemble de lignes (devis ou avenant). */
public record Totaux(BigDecimal totalHt, List<Tva> tva, BigDecimal totalTva, BigDecimal totalTtc) {

    public record Tva(BigDecimal taux, BigDecimal baseHt, BigDecimal montant) {
    }

    /** TVA calculée par taux sur la base HT cumulée, comme sur une facture. */
    public static <L> Totaux de(List<L> lignes, Function<L, BigDecimal> montantHt, Function<L, BigDecimal> tauxTva) {
        Map<BigDecimal, BigDecimal> baseParTaux = new TreeMap<>();
        BigDecimal totalHt = BigDecimal.ZERO;
        for (L ligne : lignes) {
            baseParTaux.merge(tauxTva.apply(ligne), montantHt.apply(ligne), BigDecimal::add);
            totalHt = totalHt.add(montantHt.apply(ligne));
        }
        List<Tva> tva = baseParTaux.entrySet().stream()
            .map(e -> new Tva(e.getKey(), e.getValue(), Montants.tva(e.getValue(), e.getKey())))
            .toList();
        BigDecimal totalTva = tva.stream().map(Tva::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Totaux(Montants.arrondi(totalHt), tva, Montants.arrondi(totalTva),
            Montants.arrondi(totalHt.add(totalTva)));
    }
}
