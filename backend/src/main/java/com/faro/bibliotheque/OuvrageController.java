package com.faro.bibliotheque;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
@RequestMapping("/api/ouvrages")
public class OuvrageController {

    private final OuvrageService ouvrageService;
    private final OuvrageCsvService csvService;

    public OuvrageController(OuvrageService ouvrageService, OuvrageCsvService csvService) {
        this.ouvrageService = ouvrageService;
        this.csvService = csvService;
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> export(@CurrentOrganisation Organisation organisation) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("bibliotheque-faro.csv").build().toString())
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csvService.export(organisation));
    }

    /** Import tout-ou-rien : 400 avec la liste des erreurs si une seule ligne est invalide. */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultat> importer(@CurrentOrganisation Organisation organisation,
            @RequestParam("fichier") MultipartFile fichier) throws IOException {
        ImportResultat resultat = csvService.importer(organisation, fichier.getBytes());
        return resultat.erreurs().isEmpty() ? ResponseEntity.ok(resultat) : ResponseEntity.badRequest().body(resultat);
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
