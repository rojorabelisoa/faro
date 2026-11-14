package com.faro.devis;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.faro.devis.DevisResponse.LigneResponse;
import com.faro.droits.AccesConducteur;
import com.faro.droits.Droits;
import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;

@RestController
@RequestMapping("/api/affaires/{affaireId}")
public class DevisController {

    private final DevisService devisService;
    private final DevisPdfService pdfService;
    private final DevisCsvService csvService;
    private final Droits droits;

    public DevisController(DevisService devisService, DevisPdfService pdfService, DevisCsvService csvService,
            Droits droits) {
        this.droits = droits;
        this.devisService = devisService;
        this.pdfService = pdfService;
        this.csvService = csvService;
    }

    @AccesConducteur
    @GetMapping(value = "/devis.csv", produces = "text/csv")
    public ResponseEntity<byte[]> csv(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("devis.csv").build().toString())
            .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
            .body(csvService.csv(organisation, affaireId));
    }

    @AccesConducteur
    @GetMapping("/devis")
    public DevisResponse devis(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId) {
        DevisResponse devis = devisService.devis(organisation, affaireId);
        return droits.voitLesMontantsInternes() ? devis : devis.sansDebourse();
    }

    @AccesConducteur
    @GetMapping(value = "/devis.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @RequestParam(defaultValue = "PIECE") DevisPdfService.Vue vue) {
        String fichier = "devis-" + (vue == DevisPdfService.Vue.PIECE ? "par-piece" : "par-lot") + ".pdf";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(fichier).build().toString())
            .body(pdfService.pdf(organisation, affaireId, vue));
    }

    /** Gamme retenue pour les lignes générales de l'affaire. */
    @PutMapping("/gamme")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void gammeGenerale(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @Valid @RequestBody GammeRequest request) {
        devisService.choisirGamme(organisation, affaireId, null, request.gamme());
    }

    @PutMapping("/pieces/{pieceId}/gamme")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void gammePiece(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID pieceId, @Valid @RequestBody GammeRequest request) {
        devisService.choisirGamme(organisation, affaireId, pieceId, request.gamme());
    }

    @PostMapping("/lignes")
    @ResponseStatus(HttpStatus.CREATED)
    public LigneResponse create(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @Valid @RequestBody LigneDevisRequest request) {
        return LigneResponse.from(devisService.create(organisation, affaireId, request));
    }

    @PostMapping("/lignes/depuis-ouvrage")
    @ResponseStatus(HttpStatus.CREATED)
    public LigneResponse createFromOuvrage(@CurrentOrganisation Organisation organisation,
            @PathVariable UUID affaireId, @Valid @RequestBody LigneDepuisOuvrageRequest request) {
        return LigneResponse.from(devisService.createFromOuvrage(organisation, affaireId, request));
    }

    @PutMapping("/lignes/ordre")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @Valid @RequestBody OrdreLignesRequest request) {
        devisService.reorder(organisation, affaireId, request.ids());
    }

    /** Ouvert au conducteur de travaux pour les seules lignes d'avenant (vérifié par DevisService). */
    @AccesConducteur
    @PutMapping("/lignes/{id}")
    public LigneResponse update(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id, @Valid @RequestBody LigneDevisRequest request) {
        LigneResponse ligne = LigneResponse.from(devisService.update(organisation, affaireId, id, request));
        return droits.voitLesMontantsInternes() ? ligne : ligne.sansDebourse();
    }

    @AccesConducteur
    @DeleteMapping("/lignes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id) {
        devisService.delete(organisation, affaireId, id);
    }
}
