package com.faro.estimation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RatioRepository extends JpaRepository<Ratio, UUID> {

    List<Ratio> findByOrganisationId(UUID organisationId);

    Optional<Ratio> findByIdAndOrganisationId(UUID id, UUID organisationId);
}
