package com.faro.marge;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DepenseResponse(UUID id, UUID lotId, NatureDepense nature, String libelle, String fournisseur,
        BigDecimal montantHt, LocalDate dateDepense) {

    static DepenseResponse from(Depense depense) {
        return new DepenseResponse(depense.getId(), depense.getLot().getId(), depense.getNature(), depense.getLibelle(),
            depense.getFournisseur(), depense.getMontantHt(), depense.getDateDepense());
    }
}
