package com.faro.estimation;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
public class EstimationController {

    private final EstimationService estimationService;
    private final RatioService ratioService;

    public EstimationController(EstimationService estimationService, RatioService ratioService) {
        this.estimationService = estimationService;
        this.ratioService = ratioService;
    }

    @GetMapping("/api/affaires/{affaireId}/estimation")
    public EstimationResponse estimation(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId) {
        return estimationService.estimation(organisation, affaireId);
    }

    @GetMapping("/api/ratios")
    public List<RatioResponse> ratios(@CurrentOrganisation Organisation organisation) {
        return ratioService.list(organisation);
    }

    @PutMapping("/api/ratios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@CurrentOrganisation Organisation organisation, @PathVariable UUID id,
            @Valid @RequestBody RatioRequest request) {
        ratioService.update(organisation, id, request);
    }
}
