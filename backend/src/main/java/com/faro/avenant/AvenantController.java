package com.faro.avenant;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import com.faro.devis.LigneDevisRequest;
import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
@RequestMapping("/api/affaires/{affaireId}")
public class AvenantController {

    private final AvenantService avenantService;
    private final AvenantPdfService pdfService;

    public AvenantController(AvenantService avenantService, AvenantPdfService pdfService) {
        this.avenantService = avenantService;
        this.pdfService = pdfService;
    }

    @GetMapping("/avenants")
    public List<AvenantResponse> list(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId) {
        return avenantService.list(organisation, affaireId);
    }

    @GetMapping("/marche")
    public MarcheResponse marche(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId) {
        return avenantService.marche(organisation, affaireId);
    }

    @PostMapping("/avenants")
    @ResponseStatus(HttpStatus.CREATED)
    public AvenantResponse create(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @Valid @RequestBody AvenantRequest request) {
        return avenantService.response(avenantService.create(organisation, affaireId, request));
    }

    @PutMapping("/avenants/{id}")
    public AvenantResponse update(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id, @Valid @RequestBody AvenantRequest request) {
        return avenantService.response(avenantService.update(organisation, affaireId, id, request));
    }

    @DeleteMapping("/avenants/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id) {
        avenantService.delete(organisation, affaireId, id);
    }

    /** Les lignes d'avenant se modifient et se suppriment ensuite par /lignes/{id}, tant que l'avenant est en brouillon. */
    @PostMapping("/avenants/{id}/lignes")
    @ResponseStatus(HttpStatus.CREATED)
    public LigneResponse addLigne(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id, @Valid @RequestBody LigneDevisRequest request) {
        return LigneResponse.from(avenantService.addLigne(organisation, affaireId, id, request));
    }

    @PostMapping("/avenants/{id}/envoyer")
    public AvenantResponse envoyer(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id) {
        return avenantService.response(avenantService.envoyer(organisation, affaireId, id));
    }

    @PostMapping("/avenants/{id}/decision")
    public AvenantResponse decider(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id, @Valid @RequestBody DecisionRequest request) {
        return avenantService.response(avenantService.decider(organisation, affaireId, id, request));
    }

    @GetMapping(value = "/avenants/{id}/document.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public byte[] pdf(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id) {
        return pdfService.pdf(avenantService.document(organisation, affaireId, id));
    }
}
