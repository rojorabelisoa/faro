package com.faro.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.faro.affaire.StatutAffaire;
import com.faro.avenant.AvenantResponse;
import com.faro.avenant.MarcheResponse;
import com.faro.devis.DevisResponse;

/** Ce que voit le client : son devis, ses avenants et, en chantier, le montant du marché. */
public record EspaceClientResponse(
        String entreprise,
        String affaire,
        String adresse,
        StatutAffaire statut,
        /** true : le chantier a commencé, les gammes ne sont plus modifiables. */
        boolean chantier,
        DevisResponse devis,
        List<Lot> lots,
        List<AvenantResponse> avenants,
        MarcheResponse marche) {

    public record Lot(UUID id, String code, String nom) {
    }

    public record Lien(UUID id, String code, Instant createdAt, Instant derniereVisite) {

        static Lien from(AccesClient acces) {
            return new Lien(acces.getId(), acces.getCode(), acces.getCreatedAt(), acces.getDerniereVisite());
        }
    }
}
