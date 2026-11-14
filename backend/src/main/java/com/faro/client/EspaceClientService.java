package com.faro.client;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.faro.affaire.Affaire;
import com.faro.affaire.AffaireService;
import com.faro.avenant.Avenant;
import com.faro.avenant.AvenantDocument;
import com.faro.avenant.AvenantResponse;
import com.faro.avenant.AvenantService;
import com.faro.avenant.StatutAvenant;
import com.faro.common.NotFoundException;
import com.faro.devis.DevisPdfService;
import com.faro.devis.DevisService;
import com.faro.devis.Gamme;
import com.faro.lot.LotService;
import com.faro.organisation.Organisation;

@Service
@Transactional
public class EspaceClientService {

    private static final SecureRandom ALEA = new SecureRandom();

    private final AccesClientRepository acces;
    private final AffaireService affaireService;
    private final DevisService devisService;
    private final DevisPdfService devisPdf;
    private final AvenantService avenantService;
    private final LotService lotService;
    private final Duration dureeLien;

    public EspaceClientService(AccesClientRepository acces, AffaireService affaireService, DevisService devisService,
            DevisPdfService devisPdf, AvenantService avenantService, LotService lotService,
            @Value("${faro.client.duree-lien-jours:90}") int dureeLienJours) {
        this.dureeLien = Duration.ofDays(dureeLienJours);
        this.acces = acces;
        this.affaireService = affaireService;
        this.devisService = devisService;
        this.devisPdf = devisPdf;
        this.avenantService = avenantService;
        this.lotService = lotService;
    }

    // --- Côté entreprise -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AccesClient> liens(Organisation organisation, UUID affaireId) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        return acces.findByAffaireIdAndRevoqueAtIsNullOrderByCreatedAtDesc(affaire.getId());
    }

    public AccesClient creerLien(Organisation organisation, UUID affaireId) {
        byte[] octets = new byte[32];
        ALEA.nextBytes(octets);
        return acces.save(new AccesClient(affaireService.get(organisation, affaireId),
            Base64.getUrlEncoder().withoutPadding().encodeToString(octets), Instant.now().plus(dureeLien)));
    }

    /** Repart pour la durée complète (90 jours par défaut) à partir de maintenant, même si le lien a expiré. */
    public AccesClient prolonger(Organisation organisation, UUID affaireId, UUID id) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        AccesClient lien = acces.findByIdAndAffaireId(id, affaire.getId())
            .filter(l -> l.getRevoqueAt() == null)
            .orElseThrow(() -> new NotFoundException("Lien introuvable"));
        lien.prolongerJusqua(Instant.now().plus(dureeLien));
        return lien;
    }

    public void revoquer(Organisation organisation, UUID affaireId, UUID id) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        acces.findByIdAndAffaireId(id, affaire.getId())
            .orElseThrow(() -> new NotFoundException("Lien introuvable")).revoquer();
    }

    // --- Côté client (lien secret, sans compte) --------------------------------------------------

    public EspaceClientResponse espace(String code) {
        AccesClient lien = lien(code);
        lien.visite();
        Affaire affaire = lien.getAffaire();
        Organisation organisation = affaire.getOrganisation();
        // Le déboursé sec (coût interne de l'entreprise) n'est jamais montré au client.
        List<AvenantResponse> avenants = avenantService.list(organisation, affaire.getId()).stream()
            .filter(a -> a.statut() != StatutAvenant.BROUILLON).map(AvenantResponse::sansDebourse).toList();
        return new EspaceClientResponse(organisation.getNom(), affaire.getNom(), affaire.getAdresse(), affaire.getStatut(),
            affaire.getVersionChantierId() != null, devisService.devis(organisation, affaire.getId()).sansDebourse(),
            lotService.list(organisation).stream()
                .map(l -> new EspaceClientResponse.Lot(l.getId(), l.getCode(), l.getNom())).toList(),
            avenants, avenantService.marche(organisation, affaire.getId()));
    }

    /** Le client choisit sa gamme tant que le chantier n'a pas commencé. pieceId null : lignes générales. */
    public void choisirGamme(String code, UUID pieceId, Gamme gamme) {
        Affaire affaire = lien(code).getAffaire();
        if (affaire.getVersionChantierId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le chantier a commencé : les choix sont figés");
        }
        devisService.choisirGamme(affaire.getOrganisation(), affaire.getId(), pieceId, gamme);
    }

    public void deciderAvenant(String code, UUID avenantId, StatutAvenant decision, String nom, String commentaire) {
        Affaire affaire = lien(code).getAffaire();
        Avenant avenant = avenantService.get(affaire.getOrganisation(), affaire.getId(), avenantId);
        AvenantService.enregistrerDecision(avenant, decision, nom.trim() + " (espace client)", commentaire);
    }

    @Transactional(readOnly = true)
    public byte[] devisPdf(String code) {
        Affaire affaire = lien(code).getAffaire();
        return devisPdf.pdf(affaire.getOrganisation(), affaire.getId(), DevisPdfService.Vue.PIECE);
    }

    @Transactional(readOnly = true)
    public AvenantDocument avenant(String code, UUID avenantId) {
        Affaire affaire = lien(code).getAffaire();
        return avenantService.document(affaire.getOrganisation(), affaire.getId(), avenantId);
    }

    /** Un lien expiré répond exactement comme un lien inconnu ou révoqué (404, même message). */
    private AccesClient lien(String code) {
        return acces.findByCodeAndRevoqueAtIsNull(code)
            .filter(l -> !l.estExpire(Instant.now()))
            .orElseThrow(() -> new NotFoundException("Lien invalide ou révoqué"));
    }
}
