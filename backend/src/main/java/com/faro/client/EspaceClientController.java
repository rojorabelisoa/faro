package com.faro.client;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.faro.avenant.AvenantPdfService;
import com.faro.avenant.StatutAvenant;
import com.faro.devis.Gamme;
import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
public class EspaceClientController {

    private final EspaceClientService service;
    private final AvenantPdfService avenantPdf;

    public EspaceClientController(EspaceClientService service, AvenantPdfService avenantPdf) {
        this.service = service;
        this.avenantPdf = avenantPdf;
    }

    // --- Côté entreprise (connecté) ---

    @GetMapping("/api/affaires/{affaireId}/acces-client")
    public List<EspaceClientResponse.Lien> liens(@CurrentOrganisation Organisation organisation,
            @PathVariable UUID affaireId) {
        return service.liens(organisation, affaireId).stream().map(EspaceClientResponse.Lien::from).toList();
    }

    @PostMapping("/api/affaires/{affaireId}/acces-client")
    @ResponseStatus(HttpStatus.CREATED)
    public EspaceClientResponse.Lien creer(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId) {
        return EspaceClientResponse.Lien.from(service.creerLien(organisation, affaireId));
    }

    @DeleteMapping("/api/affaires/{affaireId}/acces-client/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoquer(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id) {
        service.revoquer(organisation, affaireId, id);
    }

    // --- Côté client (public, protégé par le code secret du lien) ---

    @GetMapping("/api/public/client/{code}")
    public EspaceClientResponse espace(@PathVariable String code) {
        return service.espace(code);
    }

    public record GammeClient(@NotNull Gamme gamme) {
    }

    @PutMapping("/api/public/client/{code}/gamme")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void gammeGenerale(@PathVariable String code, @Valid @RequestBody GammeClient request) {
        service.choisirGamme(code, null, request.gamme());
    }

    @PutMapping("/api/public/client/{code}/pieces/{pieceId}/gamme")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void gammePiece(@PathVariable String code, @PathVariable UUID pieceId,
            @Valid @RequestBody GammeClient request) {
        service.choisirGamme(code, pieceId, request.gamme());
    }

    public record DecisionClient(@NotNull StatutAvenant decision, @NotBlank @Size(max = 200) String nom,
            @Size(max = 5000) String commentaire) {
    }

    @PostMapping("/api/public/client/{code}/avenants/{avenantId}/decision")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decider(@PathVariable String code, @PathVariable UUID avenantId,
            @Valid @RequestBody DecisionClient request) {
        service.deciderAvenant(code, avenantId, request.decision(), request.nom(), request.commentaire());
    }

    @GetMapping(value = "/api/public/client/{code}/devis.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public byte[] devisPdf(@PathVariable String code) {
        return service.devisPdf(code);
    }

    @GetMapping(value = "/api/public/client/{code}/avenants/{avenantId}/document.pdf",
        produces = MediaType.APPLICATION_PDF_VALUE)
    public byte[] avenantPdf(@PathVariable String code, @PathVariable UUID avenantId) {
        return avenantPdf.pdf(service.avenant(code, avenantId));
    }
}
