package com.faro.bibliotheque;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OuvrageRepository extends JpaRepository<Ouvrage, UUID> {

    /** Recherche par mot dans la désignation (insensible à la casse), éventuellement limitée à un lot. */
    @EntityGraph(attributePaths = "lot")
    @Query("""
        select o from Ouvrage o
        where o.organisation.id = :organisationId
          and (:lotId is null or o.lot.id = :lotId)
          and (:q is null or lower(o.designation) like lower(concat('%', cast(:q as string), '%')))
        order by o.lot.ordre, o.designation
        """)
    List<Ouvrage> search(@Param("organisationId") UUID organisationId, @Param("lotId") UUID lotId,
            @Param("q") String q);

    Optional<Ouvrage> findByIdAndOrganisationId(UUID id, UUID organisationId);
}
