package com.faro.version;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.faro.affaire.Affaire;
import com.faro.affaire.AffaireService;
import com.faro.affaire.StatutAffaire;
import com.faro.common.NotFoundException;
import com.faro.devis.DevisPdfService;
import com.faro.devis.DevisResponse;
import com.faro.devis.DevisService;
import com.faro.organisation.Organisation;

import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional
public class VersionService {

    static final String LIBELLE_CHANTIER = "Dossier travaux";

    private final DevisVersionRepository repository;
    private final AffaireService affaireService;
    private final DevisService devisService;
    private final DevisPdfService pdfService;
    private final JsonMapper json;

    public VersionService(DevisVersionRepository repository, AffaireService affaireService, DevisService devisService,
            DevisPdfService pdfService, JsonMapper json) {
        this.repository = repository;
        this.affaireService = affaireService;
        this.devisService = devisService;
        this.pdfService = pdfService;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> list(Organisation organisation, UUID affaireId) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        return repository.findByAffaireIdOrderByNumeroDesc(affaire.getId()).stream()
            .map(v -> VersionResponse.from(v, affaire.getVersionChantierId())).toList();
    }

    public DevisVersion figer(Organisation organisation, UUID affaireId, String libelle, String auteur) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        DevisResponse devis = devisService.devis(organisation, affaireId);
        if (devis.lignes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le devis est vide");
        }
        List<DevisVersion> versions = repository.findByAffaireIdOrderByNumeroDesc(affaire.getId());
        int numero = versions.isEmpty() ? 1 : versions.get(0).getNumero() + 1;
        String contenu = json.writeValueAsString(new VersionContenu(organisation.getNom(), affaire.getNom(),
            affaire.getClientNom(), affaire.getAdresse(), devis));
        String nom = libelle == null || libelle.isBlank() ? "Version " + numero : libelle.trim();
        return repository.save(new DevisVersion(affaire, numero, nom, contenu, devis.totalHt(), devis.totalTtc(), auteur));
    }

    /** Fige le dossier travaux, en fait la référence du chantier et passe l'affaire en chantier. */
    public DevisVersion passerEnChantier(Organisation organisation, UUID affaireId, String libelle, String auteur) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        if (affaire.getVersionChantierId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'affaire est déjà en chantier");
        }
        DevisVersion version = figer(organisation, affaireId,
            libelle == null || libelle.isBlank() ? LIBELLE_CHANTIER : libelle, auteur);
        affaire.setVersionChantierId(version.getId());
        affaire.setStatut(StatutAffaire.CHANTIER);
        return version;
    }

    @Transactional(readOnly = true)
    public DevisVersion get(Organisation organisation, UUID affaireId, UUID id) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        return repository.findByIdAndAffaireId(id, affaire.getId())
            .orElseThrow(() -> new NotFoundException("Version introuvable"));
    }

    @Transactional(readOnly = true)
    public DevisResponse devis(Organisation organisation, UUID affaireId, UUID id) {
        return contenu(get(organisation, affaireId, id)).devis();
    }

    @Transactional(readOnly = true)
    public byte[] pdf(Organisation organisation, UUID affaireId, UUID id, DevisPdfService.Vue vue) {
        DevisVersion version = get(organisation, affaireId, id);
        VersionContenu contenu = contenu(version);
        String mention = version.getLibelle() + " (version " + version.getNumero() + ") du "
            + version.getCreatedAt().atZone(ZoneId.of("Europe/Paris")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return pdfService.rendre(new DevisPdfService.EnTete(contenu.entreprise(), contenu.affaire(), contenu.client(),
            contenu.adresse(), mention), contenu.devis(), vue);
    }

    private VersionContenu contenu(DevisVersion version) {
        return json.readValue(version.getContenu(), VersionContenu.class);
    }
}
