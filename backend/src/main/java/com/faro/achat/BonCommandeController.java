package com.faro.achat;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
public class BonCommandeController {

    private final BonCommandeService service;

    public BonCommandeController(BonCommandeService service) {
        this.service = service;
    }

    @GetMapping(value = "/api/affaires/{affaireId}/lots/{lotId}/bon-commande.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public byte[] pdf(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID lotId, @RequestParam(required = false) String fournisseur) {
        return service.pdf(organisation, affaireId, lotId, fournisseur);
    }
}
