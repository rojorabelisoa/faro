package com.faro.version;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DevisVersionRepository extends JpaRepository<DevisVersion, UUID> {

    List<DevisVersion> findByAffaireIdOrderByNumeroDesc(UUID affaireId);

    Optional<DevisVersion> findByIdAndAffaireId(UUID id, UUID affaireId);
}
