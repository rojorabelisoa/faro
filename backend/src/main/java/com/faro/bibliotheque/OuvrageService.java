package com.faro.bibliotheque;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.faro.common.Montants;
import com.faro.common.NotFoundException;
import com.faro.lot.LotService;
import com.faro.organisation.Organisation;

@Service
@Transactional
public class OuvrageService {

    private final OuvrageRepository repository;
    private final LotService lotService;

    public OuvrageService(OuvrageRepository repository, LotService lotService) {
        this.repository = repository;
        this.lotService = lotService;
    }

    @Transactional(readOnly = true)
    public List<Ouvrage> search(Organisation organisation, UUID lotId, String q) {
        String terme = q == null || q.isBlank() ? null : q.trim();
        return repository.search(organisation.getId(), lotId, terme);
    }

    @Transactional(readOnly = true)
    public Ouvrage get(Organisation organisation, UUID id) {
        return repository.findByIdAndOrganisationId(id, organisation.getId())
            .orElseThrow(() -> new NotFoundException("Ouvrage introuvable"));
    }

    public Ouvrage create(Organisation organisation, OuvrageRequest request) {
        Ouvrage ouvrage = new Ouvrage(organisation);
        apply(organisation, ouvrage, request);
        return repository.save(ouvrage);
    }

    public Ouvrage update(Organisation organisation, UUID id, OuvrageRequest request) {
        Ouvrage ouvrage = get(organisation, id);
        apply(organisation, ouvrage, request);
        return ouvrage;
    }

    public void delete(Organisation organisation, UUID id) {
        repository.delete(get(organisation, id));
    }

    private void apply(Organisation organisation, Ouvrage ouvrage, OuvrageRequest request) {
        ouvrage.setLot(lotService.get(organisation, request.lotId()));
        ouvrage.setDesignation(request.designation());
        ouvrage.setDescription(request.description());
        ouvrage.setUnite(request.unite());
        ouvrage.setPrixUnitaireHt(request.prixUnitaireHt());
        ouvrage.setTauxTva(Montants.tauxTva(request.tauxTva()));
    }
}
