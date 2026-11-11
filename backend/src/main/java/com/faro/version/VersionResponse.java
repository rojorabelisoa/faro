package com.faro.version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VersionResponse(UUID id, int numero, String libelle, BigDecimal totalHt, BigDecimal totalTtc,
        String auteur, Instant createdAt, boolean chantier) {

    static VersionResponse from(DevisVersion version, UUID versionChantierId) {
        return new VersionResponse(version.getId(), version.getNumero(), version.getLibelle(), version.getTotalHt(),
            version.getTotalTtc(), version.getAuteur(), version.getCreatedAt(), version.getId().equals(versionChantierId));
    }
}
