package com.faro.affaire;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Données saisies pour créer ou modifier une affaire. Le statut est ignoré à la création. */
public record AffaireRequest(
        @NotBlank @Size(max = 255) String nom,
        @Size(max = 255) String clientNom,
        @Email @Size(max = 320) String clientEmail,
        @Size(max = 50) String clientTelephone,
        @Size(max = 500) String adresse,
        @NotNull TypeBien typeBien,
        StatutAffaire statut,
        String notes) {
}
