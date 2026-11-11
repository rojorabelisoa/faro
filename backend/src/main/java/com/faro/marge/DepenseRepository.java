package com.faro.marge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepenseRepository extends JpaRepository<Depense, UUID> {

    @EntityGraph(attributePaths = "lot")
    List<Depense> findByAffaireIdOrderByDateDepenseAscCreatedAtAsc(UUID affaireId);

    Optional<Depense> findByIdAndAffaireId(UUID id, UUID affaireId);
}
