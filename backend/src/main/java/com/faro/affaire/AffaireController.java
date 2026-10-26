package com.faro.affaire;

import java.util.List;
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

import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
@RequestMapping("/api/affaires")
public class AffaireController {

    private final AffaireService affaireService;

    public AffaireController(AffaireService affaireService) {
        this.affaireService = affaireService;
    }

    @GetMapping
    public List<AffaireResponse> list(@CurrentOrganisation Organisation organisation) {
        return affaireService.list(organisation).stream().map(AffaireResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AffaireResponse get(@CurrentOrganisation Organisation organisation, @PathVariable UUID id) {
        return AffaireResponse.from(affaireService.get(organisation, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AffaireResponse create(@CurrentOrganisation Organisation organisation,
            @Valid @RequestBody AffaireRequest request) {
        return AffaireResponse.from(affaireService.create(organisation, request));
    }

    @PutMapping("/{id}")
    public AffaireResponse update(@CurrentOrganisation Organisation organisation, @PathVariable UUID id,
            @Valid @RequestBody AffaireRequest request) {
        return AffaireResponse.from(affaireService.update(organisation, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentOrganisation Organisation organisation, @PathVariable UUID id) {
        affaireService.delete(organisation, id);
    }
}
