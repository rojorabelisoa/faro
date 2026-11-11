package com.faro.visite;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.faro.visite.PhotoRepository.PhotoInfo;

public record VisiteResponse(List<ObservationResponse> observations, List<PhotoInfo> photos) {

    public record ObservationResponse(UUID id, UUID pieceId, String texte, String auteur, Instant createdAt) {

        static ObservationResponse from(Observation observation) {
            return new ObservationResponse(observation.getId(),
                observation.getPiece() != null ? observation.getPiece().getId() : null,
                observation.getTexte(), observation.getAuteur(), observation.getCreatedAt());
        }
    }
}
