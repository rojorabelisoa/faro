package com.faro.lot;

import java.util.List;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.faro.common.NotFoundException;
import com.faro.organisation.Organisation;
import com.faro.organisation.OrganisationCreatedEvent;

@Service
@Transactional
public class LotService {

    /** Lots proposés à toute nouvelle entreprise de rénovation ; modifiables ensuite. */
    static final List<String[]> LOTS_PAR_DEFAUT = List.of(
        new String[] {"01", "Installation de chantier / Protections"},
        new String[] {"02", "Démolition / Dépose"},
        new String[] {"03", "Maçonnerie / Gros œuvre"},
        new String[] {"04", "Plâtrerie / Isolation"},
        new String[] {"05", "Menuiseries intérieures"},
        new String[] {"06", "Menuiseries extérieures"},
        new String[] {"07", "Plomberie / Sanitaires"},
        new String[] {"08", "Électricité"},
        new String[] {"09", "Chauffage / Ventilation"},
        new String[] {"10", "Carrelage / Faïence"},
        new String[] {"11", "Revêtements de sols"},
        new String[] {"12", "Peinture / Revêtements muraux"},
        new String[] {"13", "Cuisine / Agencement"},
        new String[] {"14", "Nettoyage / Évacuation des gravats"});

    private final LotRepository repository;

    public LotService(LotRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onOrganisationCreated(OrganisationCreatedEvent event) {
        for (int i = 0; i < LOTS_PAR_DEFAUT.size(); i++) {
            String[] lot = LOTS_PAR_DEFAUT.get(i);
            repository.save(new Lot(event.organisation(), lot[0], lot[1], i + 1));
        }
    }

    @Transactional(readOnly = true)
    public List<Lot> list(Organisation organisation) {
        return repository.findByOrganisationIdOrderByOrdreAscCodeAsc(organisation.getId());
    }

    @Transactional(readOnly = true)
    public Lot get(Organisation organisation, UUID id) {
        return repository.findByIdAndOrganisationId(id, organisation.getId())
            .orElseThrow(() -> new NotFoundException("Lot introuvable"));
    }

    public Lot create(Organisation organisation, LotRequest request) {
        checkCodeIsFree(organisation, request.code());
        int ordre = request.ordre() != null ? request.ordre()
            : list(organisation).stream().mapToInt(Lot::getOrdre).max().orElse(0) + 1;
        return repository.save(new Lot(organisation, request.code(), request.nom(), ordre));
    }

    public Lot update(Organisation organisation, UUID id, LotRequest request) {
        Lot lot = get(organisation, id);
        if (!lot.getCode().equals(request.code())) {
            checkCodeIsFree(organisation, request.code());
        }
        lot.setCode(request.code());
        lot.setNom(request.nom());
        if (request.ordre() != null) {
            lot.setOrdre(request.ordre());
        }
        return lot;
    }

    public void delete(Organisation organisation, UUID id) {
        repository.delete(get(organisation, id));
    }

    private void checkCodeIsFree(Organisation organisation, String code) {
        if (repository.existsByOrganisationIdAndCode(organisation.getId(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un lot avec le code " + code + " existe déjà");
        }
    }
}
