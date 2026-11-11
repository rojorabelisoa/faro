package com.faro.investisseur;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.faro.investisseur.BilanDtos.BilanRequest;
import com.faro.investisseur.BilanDtos.BilanResponse;
import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
public class BilanController {

    private final BilanService bilanService;

    public BilanController(BilanService bilanService) {
        this.bilanService = bilanService;
    }

    @GetMapping("/api/affaires/{affaireId}/bilan")
    public BilanResponse bilan(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId) {
        return bilanService.bilan(organisation, affaireId);
    }

    @PutMapping("/api/affaires/{affaireId}/bilan")
    public BilanResponse enregistrer(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @Valid @RequestBody BilanRequest request) {
        return bilanService.enregistrer(organisation, affaireId, request);
    }
}
