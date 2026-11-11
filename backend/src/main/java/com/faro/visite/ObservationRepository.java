package com.faro.visite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservationRepository extends JpaRepository<Observation, UUID> {

    List<Observation> findByAffaireIdOrderByCreatedAtAsc(UUID affaireId);

    Optional<Observation> findByIdAndAffaireId(UUID id, UUID affaireId);
}
