package com.faro.visite;

import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.faro.affaire.Affaire;
import com.faro.affaire.AffaireService;
import com.faro.affaire.Piece;
import com.faro.affaire.PieceService;
import com.faro.common.NotFoundException;
import com.faro.organisation.Organisation;
import com.faro.visite.VisiteResponse.ObservationResponse;

@Service
@Transactional
public class VisiteService {

    static final Set<String> TYPES_IMAGE = Set.of("image/jpeg", "image/png", "image/webp");

    private final ObservationRepository observations;
    private final PhotoRepository photos;
    private final AffaireService affaireService;
    private final PieceService pieceService;

    public VisiteService(ObservationRepository observations, PhotoRepository photos, AffaireService affaireService,
            PieceService pieceService) {
        this.observations = observations;
        this.photos = photos;
        this.affaireService = affaireService;
        this.pieceService = pieceService;
    }

    @Transactional(readOnly = true)
    public VisiteResponse visite(Organisation organisation, UUID affaireId) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        return new VisiteResponse(
            observations.findByAffaireIdOrderByCreatedAtAsc(affaire.getId()).stream().map(ObservationResponse::from).toList(),
            photos.findInfos(affaire.getId()));
    }

    public Observation addObservation(Organisation organisation, UUID affaireId, ObservationRequest request,
            String auteur) {
        Observation observation = new Observation(affaireService.get(organisation, affaireId), auteur);
        observation.setPiece(piece(organisation, affaireId, request.pieceId()));
        observation.setTexte(request.texte().trim());
        return observations.save(observation);
    }

    public Observation updateObservation(Organisation organisation, UUID affaireId, UUID id, ObservationRequest request) {
        Observation observation = observation(organisation, affaireId, id);
        observation.setPiece(piece(organisation, affaireId, request.pieceId()));
        observation.setTexte(request.texte().trim());
        return observation;
    }

    public void deleteObservation(Organisation organisation, UUID affaireId, UUID id) {
        observations.delete(observation(organisation, affaireId, id));
    }

    public Photo addPhoto(Organisation organisation, UUID affaireId, UUID pieceId, String legende, String contentType,
            byte[] donnees) {
        if (contentType == null || !TYPES_IMAGE.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formats acceptés : JPEG, PNG, WebP");
        }
        if (donnees.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo vide");
        }
        String texte = legende == null || legende.isBlank() ? null : legende.trim();
        return photos.save(new Photo(affaireService.get(organisation, affaireId), piece(organisation, affaireId, pieceId),
            texte, contentType, donnees));
    }

    @Transactional(readOnly = true)
    public Photo photo(Organisation organisation, UUID affaireId, UUID id) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        return photos.findByIdAndAffaireId(id, affaire.getId())
            .orElseThrow(() -> new NotFoundException("Photo introuvable"));
    }

    public void deletePhoto(Organisation organisation, UUID affaireId, UUID id) {
        photos.delete(photo(organisation, affaireId, id));
    }

    private Observation observation(Organisation organisation, UUID affaireId, UUID id) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        return observations.findByIdAndAffaireId(id, affaire.getId())
            .orElseThrow(() -> new NotFoundException("Observation introuvable"));
    }

    private Piece piece(Organisation organisation, UUID affaireId, UUID pieceId) {
        return pieceId != null ? pieceService.get(organisation, affaireId, pieceId) : null;
    }
}
