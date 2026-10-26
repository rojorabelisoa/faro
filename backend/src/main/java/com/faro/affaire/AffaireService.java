package com.faro.affaire;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.faro.common.NotFoundException;
import com.faro.organisation.Organisation;

@Service
@Transactional
public class AffaireService {

    private final AffaireRepository repository;

    public AffaireService(AffaireRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Affaire> list(Organisation organisation) {
        return repository.findByOrganisationIdOrderByUpdatedAtDesc(organisation.getId());
    }

    /** Affaire de l'organisation, sinon 404 (y compris si elle appartient à une autre organisation). */
    @Transactional(readOnly = true)
    public Affaire get(Organisation organisation, UUID id) {
        return repository.findByIdAndOrganisationId(id, organisation.getId())
            .orElseThrow(() -> new NotFoundException("Affaire introuvable"));
    }

    public Affaire create(Organisation organisation, AffaireRequest request) {
        Affaire affaire = new Affaire(organisation, request.nom(), request.typeBien());
        apply(affaire, request);
        return repository.save(affaire);
    }

    public Affaire update(Organisation organisation, UUID id, AffaireRequest request) {
        Affaire affaire = get(organisation, id);
        affaire.setNom(request.nom());
        affaire.setTypeBien(request.typeBien());
        if (request.statut() != null) {
            affaire.setStatut(request.statut());
        }
        apply(affaire, request);
        return affaire;
    }

    public void delete(Organisation organisation, UUID id) {
        repository.delete(get(organisation, id));
    }

    private static void apply(Affaire affaire, AffaireRequest request) {
        affaire.setClientNom(request.clientNom());
        affaire.setClientEmail(request.clientEmail());
        affaire.setClientTelephone(request.clientTelephone());
        affaire.setAdresse(request.adresse());
        affaire.setNotes(request.notes());
    }
}
