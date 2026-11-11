package com.faro.equipe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    List<Invitation> findByOrganisationIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID organisationId);

    Optional<Invitation> findByCode(String code);

    Optional<Invitation> findByIdAndOrganisationId(UUID id, UUID organisationId);
}
