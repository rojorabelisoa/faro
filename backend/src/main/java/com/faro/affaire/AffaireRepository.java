package com.faro.affaire;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AffaireRepository extends JpaRepository<Affaire, UUID> {

    List<Affaire> findByOrganisationIdOrderByUpdatedAtDesc(UUID organisationId);

    Optional<Affaire> findByIdAndOrganisationId(UUID id, UUID organisationId);
}
