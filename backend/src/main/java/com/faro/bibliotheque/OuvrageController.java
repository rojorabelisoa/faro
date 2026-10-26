package com.faro.bibliotheque;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
@RequestMapping("/api/ouvrages")
public class OuvrageController {

    private final OuvrageService ouvrageService;

    public OuvrageController(OuvrageService ouvrageService) {
        this.ouvrageService = ouvrageService;
    }

    @GetMapping
    public List<OuvrageResponse> search(@CurrentOrganisation Organisation organisation,
            @RequestParam(required = false) UUID lotId, @RequestParam(required = false) String q) {
        return ouvrageService.search(organisation, lotId, q).stream().map(OuvrageResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OuvrageResponse create(@CurrentOrganisation Organisation organisation,
            @Valid @RequestBody OuvrageRequest request) {
        return OuvrageResponse.from(ouvrageService.create(organisation, request));
    }

    @PutMapping("/{id}")
    public OuvrageResponse update(@CurrentOrganisation Organisation organisation, @PathVariable UUID id,
            @Valid @RequestBody OuvrageRequest request) {
        return OuvrageResponse.from(ouvrageService.update(organisation, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentOrganisation Organisation organisation, @PathVariable UUID id) {
        ouvrageService.delete(organisation, id);
    }
}
