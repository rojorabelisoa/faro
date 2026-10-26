package com.faro.lot;

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
@RequestMapping("/api/lots")
public class LotController {

    private final LotService lotService;

    public LotController(LotService lotService) {
        this.lotService = lotService;
    }

    @GetMapping
    public List<LotResponse> list(@CurrentOrganisation Organisation organisation) {
        return lotService.list(organisation).stream().map(LotResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LotResponse create(@CurrentOrganisation Organisation organisation, @Valid @RequestBody LotRequest request) {
        return LotResponse.from(lotService.create(organisation, request));
    }

    @PutMapping("/{id}")
    public LotResponse update(@CurrentOrganisation Organisation organisation, @PathVariable UUID id,
            @Valid @RequestBody LotRequest request) {
        return LotResponse.from(lotService.update(organisation, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentOrganisation Organisation organisation, @PathVariable UUID id) {
        lotService.delete(organisation, id);
    }
}
