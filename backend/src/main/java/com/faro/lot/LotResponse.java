package com.faro.lot;

import java.util.UUID;

public record LotResponse(UUID id, String code, String nom, int ordre) {

    public static LotResponse from(Lot lot) {
        return new LotResponse(lot.getId(), lot.getCode(), lot.getNom(), lot.getOrdre());
    }
}
