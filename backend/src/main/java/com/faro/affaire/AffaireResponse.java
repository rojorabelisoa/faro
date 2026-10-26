package com.faro.affaire;

import java.time.Instant;
import java.util.UUID;

public record AffaireResponse(
        UUID id,
        String nom,
        String clientNom,
        String clientEmail,
        String clientTelephone,
        String adresse,
        TypeBien typeBien,
        StatutAffaire statut,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    static AffaireResponse from(Affaire affaire) {
        return new AffaireResponse(affaire.getId(), affaire.getNom(), affaire.getClientNom(),
            affaire.getClientEmail(), affaire.getClientTelephone(), affaire.getAdresse(),
            affaire.getTypeBien(), affaire.getStatut(), affaire.getNotes(),
            affaire.getCreatedAt(), affaire.getUpdatedAt());
    }
}
