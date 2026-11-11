package com.faro.marge;

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
@RequestMapping("/api/affaires/{affaireId}")
public class MargeController {

    private final MargeService margeService;

    public MargeController(MargeService margeService) {
        this.margeService = margeService;
    }

    @GetMapping("/marge")
    public MargeResponse marge(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId) {
        return margeService.marge(organisation, affaireId);
    }

    @PostMapping("/depenses")
    @ResponseStatus(HttpStatus.CREATED)
    public DepenseResponse add(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @Valid @RequestBody DepenseRequest request) {
        return DepenseResponse.from(margeService.addDepense(organisation, affaireId, request));
    }

    @PutMapping("/depenses/{id}")
    public DepenseResponse update(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id, @Valid @RequestBody DepenseRequest request) {
        return DepenseResponse.from(margeService.updateDepense(organisation, affaireId, id, request));
    }

    @DeleteMapping("/depenses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id) {
        margeService.deleteDepense(organisation, affaireId, id);
    }
}
