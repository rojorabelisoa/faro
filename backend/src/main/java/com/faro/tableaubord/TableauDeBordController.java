package com.faro.tableaubord;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.faro.droits.AccesConducteur;
import com.faro.droits.Droits;
import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
public class TableauDeBordController {

    private final TableauDeBordService service;
    private final Droits droits;

    public TableauDeBordController(TableauDeBordService service, Droits droits) {
        this.droits = droits;
        this.service = service;
    }

    @AccesConducteur
    @GetMapping("/api/tableau-de-bord")
    public TableauDeBordResponse tableau(@CurrentOrganisation Organisation organisation) {
        TableauDeBordResponse tableau = service.tableau(organisation, Instant.now());
        return droits.voitLesMontantsInternes() ? tableau : tableau.sansMontants();
    }
}
