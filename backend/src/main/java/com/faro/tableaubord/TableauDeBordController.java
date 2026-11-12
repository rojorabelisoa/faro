package com.faro.tableaubord;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
public class TableauDeBordController {

    private final TableauDeBordService service;

    public TableauDeBordController(TableauDeBordService service) {
        this.service = service;
    }

    @GetMapping("/api/tableau-de-bord")
    public TableauDeBordResponse tableau(@CurrentOrganisation Organisation organisation) {
        return service.tableau(organisation, Instant.now());
    }
}
