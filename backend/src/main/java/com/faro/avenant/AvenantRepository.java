package com.faro.avenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AvenantRepository extends JpaRepository<Avenant, UUID> {

    List<Avenant> findByAffaireIdOrderByNumeroAsc(UUID affaireId);

    Optional<Avenant> findByIdAndAffaireId(UUID id, UUID affaireId);
}
