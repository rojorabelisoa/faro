package com.faro.devis;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LigneDevisRepository extends JpaRepository<LigneDevis, UUID> {

    /** Lignes du devis de base (hors avenants). */
    @EntityGraph(attributePaths = {"piece", "lot"})
    List<LigneDevis> findByAffaireIdAndAvenantIsNullOrderByOrdreAsc(UUID affaireId);

    @EntityGraph(attributePaths = {"piece", "lot"})
    List<LigneDevis> findByAvenantIdOrderByOrdreAsc(UUID avenantId);

    Optional<LigneDevis> findByIdAndAffaireId(UUID id, UUID affaireId);
}
