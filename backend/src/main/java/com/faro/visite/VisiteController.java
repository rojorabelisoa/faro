package com.faro.visite;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import org.springframework.web.multipart.MultipartFile;

import com.faro.organisation.CurrentOrganisation;
import com.faro.organisation.Organisation;
import com.faro.visite.PhotoRepository.PhotoInfo;
import com.faro.visite.VisiteResponse.ObservationResponse;

@RestController
@RequestMapping("/api/affaires/{affaireId}")
public class VisiteController {

    private final VisiteService visiteService;
    private final VisitePdfService pdfService;

    public VisiteController(VisiteService visiteService, VisitePdfService pdfService) {
        this.visiteService = visiteService;
        this.pdfService = pdfService;
    }

    @GetMapping("/visite")
    public VisiteResponse visite(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId) {
        return visiteService.visite(organisation, affaireId);
    }

    @GetMapping(value = "/visite.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public byte[] pdf(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId) {
        return pdfService.pdf(organisation, affaireId);
    }

    @PostMapping("/observations")
    @ResponseStatus(HttpStatus.CREATED)
    public ObservationResponse addObservation(@CurrentOrganisation Organisation organisation,
            @PathVariable UUID affaireId, @Valid @RequestBody ObservationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ObservationResponse.from(visiteService.addObservation(organisation, affaireId, request, auteur(jwt)));
    }

    @PutMapping("/observations/{id}")
    public ObservationResponse updateObservation(@CurrentOrganisation Organisation organisation,
            @PathVariable UUID affaireId, @PathVariable UUID id, @Valid @RequestBody ObservationRequest request) {
        return ObservationResponse.from(visiteService.updateObservation(organisation, affaireId, id, request));
    }

    @DeleteMapping("/observations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteObservation(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id) {
        visiteService.deleteObservation(organisation, affaireId, id);
    }

    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoInfo addPhoto(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @RequestParam("fichier") MultipartFile fichier, @RequestParam(required = false) UUID pieceId,
            @RequestParam(required = false) String legende) throws IOException {
        Photo photo = visiteService.addPhoto(organisation, affaireId, pieceId, legende, fichier.getContentType(),
            fichier.getBytes());
        return new PhotoInfo(photo.getId(), pieceId, photo.getLegende(), photo.getContentType(), photo.getTaille(),
            photo.getCreatedAt());
    }

    @GetMapping("/photos/{id}")
    public ResponseEntity<byte[]> photo(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id) {
        Photo photo = visiteService.photo(organisation, affaireId, id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.getContentType()))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
            .body(photo.getDonnees());
    }

    @DeleteMapping("/photos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@CurrentOrganisation Organisation organisation, @PathVariable UUID affaireId,
            @PathVariable UUID id) {
        visiteService.deletePhoto(organisation, affaireId, id);
    }

    private static String auteur(Jwt jwt) {
        String nom = jwt.getClaimAsString("name");
        return nom != null ? nom : jwt.getClaimAsString("preferred_username");
    }
}
