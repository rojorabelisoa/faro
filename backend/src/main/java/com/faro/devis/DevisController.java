package com.faro.devis;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.faro.devis.DevisResponse.LigneResponse;
import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
@RequestMapping("/api/affaires/{affaireId}")
public class DevisController {

    private final DevisService devisService;

    public DevisController(DevisService devisService) {
        this.devisService = devisService;
    }

    @GetMapping("/devis")
    public DevisResponse devis(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId) {
        return devisService.devis(organisation, affaireId);
    }

    @PostMapping("/lignes")
    @ResponseStatus(HttpStatus.CREATED)
    public LigneResponse create(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @Valid @RequestBody LigneDevisRequest request) {
        return LigneResponse.from(devisService.create(organisation, affaireId, request));
    }

    @PostMapping("/lignes/depuis-ouvrage")
    @ResponseStatus(HttpStatus.CREATED)
    public LigneResponse createFromOuvrage(@CurrentOrganisation Organisation organisation,
            @PathVariable UUID affaireId, @Valid @RequestBody LigneDepuisOuvrageRequest request) {
        return LigneResponse.from(devisService.createFromOuvrage(organisation, affaireId, request));
    }

    @PutMapping("/lignes/{id}")
    public LigneResponse update(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id, @Valid @RequestBody LigneDevisRequest request) {
        return LigneResponse.from(devisService.update(organisation, affaireId, id, request));
    }

    @DeleteMapping("/lignes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id) {
        devisService.delete(organisation, affaireId, id);
    }
}
