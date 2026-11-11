package com.faro.version;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.faro.devis.DevisPdfService;
import com.faro.devis.DevisResponse;
import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
@RequestMapping("/api/affaires/{affaireId}")
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping("/versions")
    public List<VersionResponse> list(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId) {
        return versionService.list(organisation, affaireId);
    }

    @PostMapping("/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public VersionResponse figer(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @Valid @RequestBody(required = false) VersionRequest request, @AuthenticationPrincipal Jwt jwt) {
        DevisVersion version = versionService.figer(organisation, affaireId, request == null ? null : request.libelle(),
            auteur(jwt));
        return VersionResponse.from(version, null);
    }

    @PostMapping("/chantier")
    @ResponseStatus(HttpStatus.CREATED)
    public VersionResponse passerEnChantier(@CurrentOrganisation Organisation organisation,
            @PathVariable UUID affaireId, @Valid @RequestBody(required = false) VersionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        DevisVersion version = versionService.passerEnChantier(organisation, affaireId,
            request == null ? null : request.libelle(), auteur(jwt));
        return VersionResponse.from(version, version.getId());
    }

    @GetMapping("/versions/{id}/devis")
    public DevisResponse devis(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id) {
        return versionService.devis(organisation, affaireId, id);
    }

    @GetMapping(value = "/versions/{id}/devis.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public byte[] pdf(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id, @RequestParam(defaultValue = "PIECE") DevisPdfService.Vue vue) {
        return versionService.pdf(organisation, affaireId, id, vue);
    }

    static String auteur(Jwt jwt) {
        String nom = jwt.getClaimAsString("name");
        return nom != null ? nom : jwt.getClaimAsString("preferred_username");
    }
}
