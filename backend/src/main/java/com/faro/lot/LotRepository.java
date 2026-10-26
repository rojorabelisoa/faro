package com.faro.lot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LotRepository extends JpaRepository<Lot, UUID> {

    List<Lot> findByOrganisationIdOrderByOrdreAscCodeAsc(UUID organisationId);

    Optional<Lot> findByIdAndOrganisationId(UUID id, UUID organisationId);

    boolean existsByOrganisationIdAndCode(UUID organisationId, String code);
}
