package com.faro.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccesClientRepository extends JpaRepository<AccesClient, UUID> {

    List<AccesClient> findByAffaireIdAndRevoqueAtIsNullOrderByCreatedAtDesc(UUID affaireId);

    Optional<AccesClient> findByCodeAndRevoqueAtIsNull(String code);

    Optional<AccesClient> findByIdAndAffaireId(UUID id, UUID affaireId);
}
