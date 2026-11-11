package com.faro.avenant;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.faro.affaire.Affaire;
import com.faro.affaire.AffaireService;
import com.faro.affaire.PieceService;
import com.faro.common.NotFoundException;
import com.faro.common.Totaux;
import com.faro.devis.DevisResponse;
import com.faro.devis.DevisResponse.LigneResponse;
import com.faro.devis.DevisService;
import com.faro.devis.LigneDevis;
import com.faro.devis.LigneDevisRepository;
import com.faro.devis.LigneDevisRequest;
import com.faro.lot.LotService;
import com.faro.organisation.Organisation;
import com.faro.version.DevisVersion;
import com.faro.version.DevisVersionRepository;

import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional
public class AvenantService {

    private final AvenantRepository repository;
    private final LigneDevisRepository lignes;
    private final AffaireService affaireService;
    private final DevisService devisService;
    private final PieceService pieceService;
    private final LotService lotService;
    private final DevisVersionRepository versions;
    private final JsonMapper json;

    public AvenantService(AvenantRepository repository, LigneDevisRepository lignes, AffaireService affaireService,
            DevisService devisService, PieceService pieceService, LotService lotService,
            DevisVersionRepository versions, JsonMapper json) {
        this.repository = repository;
        this.lignes = lignes;
        this.affaireService = affaireService;
        this.devisService = devisService;
        this.pieceService = pieceService;
        this.lotService = lotService;
        this.versions = versions;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<AvenantResponse> list(Organisation organisation, UUID affaireId) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        return repository.findByAffaireIdOrderByNumeroAsc(affaire.getId()).stream().map(this::response).toList();
    }

    public Avenant create(Organisation organisation, UUID affaireId, AvenantRequest request) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        int numero = repository.findByAffaireIdOrderByNumeroAsc(affaire.getId()).size() + 1;
        return repository.save(new Avenant(affaire, numero, request.objet().trim(), texte(request.motif())));
    }

    public Avenant update(Organisation organisation, UUID affaireId, UUID id, AvenantRequest request) {
        Avenant avenant = brouillon(organisation, affaireId, id);
        avenant.setObjet(request.objet().trim());
        avenant.setMotif(texte(request.motif()));
        return avenant;
    }

    public void delete(Organisation organisation, UUID affaireId, UUID id) {
        repository.delete(brouillon(organisation, affaireId, id));
    }

    public LigneDevis addLigne(Organisation organisation, UUID affaireId, UUID id, LigneDevisRequest request) {
        return devisService.createForAvenant(organisation, affaireId, brouillon(organisation, affaireId, id), request);
    }

    /** Fige le contenu envoyé au client : les lignes ne sont plus modifiables. */
    public Avenant envoyer(Organisation organisation, UUID affaireId, UUID id) {
        Avenant avenant = brouillon(organisation, affaireId, id);
        AvenantResponse contenu = response(avenant);
        if (contenu.lignes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'avenant n'a aucune ligne");
        }
        Affaire affaire = avenant.getAffaire();
        Map<UUID, String> pieces = new HashMap<>();
        pieceService.list(organisation, affaireId).forEach(p -> pieces.put(p.getId(), p.getNom()));
        Map<UUID, String> lots = new HashMap<>();
        lotService.list(organisation).forEach(l -> lots.put(l.getId(), l.getCode() + " · " + l.getNom()));
        avenant.envoyer(json.writeValueAsString(new AvenantDocument(organisation.getNom(), affaire.getNom(),
            affaire.getClientNom(), affaire.getAdresse(), contenu, pieces, lots)));
        return avenant;
    }

    public Avenant decider(Organisation organisation, UUID affaireId, UUID id, DecisionRequest request) {
        Avenant avenant = get(organisation, affaireId, id);
        enregistrerDecision(avenant, request.decision(), request.par(), request.commentaire());
        return avenant;
    }

    /** Aussi utilisée par l'espace client : seule une décision sur un avenant envoyé est possible. */
    public static void enregistrerDecision(Avenant avenant, StatutAvenant decision, String par, String commentaire) {
        if (avenant.getStatut() != StatutAvenant.ENVOYE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seul un avenant envoyé au client peut être validé ou refusé");
        }
        if (decision != StatutAvenant.VALIDE && decision != StatutAvenant.REFUSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Décision attendue : VALIDE ou REFUSE");
        }
        avenant.decider(decision, par.trim(), texte(commentaire));
    }

    @Transactional(readOnly = true)
    public AvenantDocument document(Organisation organisation, UUID affaireId, UUID id) {
        Avenant avenant = get(organisation, affaireId, id);
        if (avenant.getContenuEnvoye() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Envoyez l'avenant pour générer son document");
        }
        AvenantDocument document = json.readValue(avenant.getContenuEnvoye(), AvenantDocument.class);
        // Le contenu est celui envoyé ; le statut et la décision sont ceux d'aujourd'hui.
        AvenantResponse actuel = response(avenant);
        return new AvenantDocument(document.entreprise(), document.affaire(), document.client(), document.adresse(),
            new AvenantResponse(actuel.id(), actuel.numero(), document.avenant().objet(), document.avenant().motif(),
                actuel.statut(), document.avenant().lignes(), document.avenant().totaux(), actuel.createdAt(),
                actuel.envoyeAt(), actuel.decideAt(), actuel.decidePar(), actuel.decisionCommentaire()),
            document.pieces(), document.lots());
    }

    @Transactional(readOnly = true)
    public MarcheResponse marche(Organisation organisation, UUID affaireId) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        BigDecimal initialHt;
        BigDecimal initialTtc;
        DevisVersion reference = affaire.getVersionChantierId() == null ? null
            : versions.findById(affaire.getVersionChantierId()).orElse(null);
        if (reference != null) {
            initialHt = reference.getTotalHt();
            initialTtc = reference.getTotalTtc();
        } else {
            DevisResponse devis = devisService.devis(organisation, affaireId);
            initialHt = devis.totalHt();
            initialTtc = devis.totalTtc();
        }
        BigDecimal validesHt = BigDecimal.ZERO;
        BigDecimal validesTtc = BigDecimal.ZERO;
        BigDecimal attenteHt = BigDecimal.ZERO;
        for (AvenantResponse avenant : list(organisation, affaireId)) {
            if (avenant.statut() == StatutAvenant.VALIDE) {
                validesHt = validesHt.add(avenant.totaux().totalHt());
                validesTtc = validesTtc.add(avenant.totaux().totalTtc());
            } else if (avenant.statut() == StatutAvenant.ENVOYE) {
                attenteHt = attenteHt.add(avenant.totaux().totalHt());
            }
        }
        return new MarcheResponse(reference != null, initialHt, initialTtc, validesHt, validesTtc,
            initialHt.add(validesHt), initialTtc.add(validesTtc), attenteHt);
    }

    public Avenant get(Organisation organisation, UUID affaireId, UUID id) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        return repository.findByIdAndAffaireId(id, affaire.getId())
            .orElseThrow(() -> new NotFoundException("Avenant introuvable"));
    }

    public AvenantResponse response(Avenant avenant) {
        List<LigneDevis> lignesAvenant = lignes.findByAvenantIdOrderByOrdreAsc(avenant.getId());
        return new AvenantResponse(avenant.getId(), avenant.getNumero(), avenant.getObjet(), avenant.getMotif(),
            avenant.getStatut(), lignesAvenant.stream().map(LigneResponse::from).toList(),
            Totaux.de(lignesAvenant, LigneDevis::getMontantHt, LigneDevis::getTauxTva), avenant.getCreatedAt(),
            avenant.getEnvoyeAt(), avenant.getDecideAt(), avenant.getDecidePar(), avenant.getDecisionCommentaire());
    }

    private Avenant brouillon(Organisation organisation, UUID affaireId, UUID id) {
        Avenant avenant = get(organisation, affaireId, id);
        if (avenant.getStatut() != StatutAvenant.BROUILLON) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'avenant a été envoyé : il n'est plus modifiable");
        }
        return avenant;
    }

    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
