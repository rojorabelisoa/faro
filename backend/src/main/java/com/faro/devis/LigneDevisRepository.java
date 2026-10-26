package com.faro.devis;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LigneDevisRepository extends JpaRepository<LigneDevis, UUID> {

    @EntityGraph(attributePaths = {"piece", "lot"})
    List<LigneDevis> findByAffaireIdOrderByOrdreAsc(UUID affaireId);

    Optional<LigneDevis> findByIdAndAffaireId(UUID id, UUID affaireId);
}
