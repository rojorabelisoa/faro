package com.faro.visite;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    /** Les métadonnées seulement : les images sont chargées une par une. */
    @Query("""
        select new com.faro.visite.PhotoRepository$PhotoInfo(p.id, p.piece.id, p.legende, p.contentType, p.taille, p.createdAt)
        from Photo p left join p.piece where p.affaire.id = :affaireId order by p.createdAt
        """)
    List<PhotoInfo> findInfos(UUID affaireId);

    List<Photo> findByAffaireIdOrderByCreatedAtAsc(UUID affaireId);

    Optional<Photo> findByIdAndAffaireId(UUID id, UUID affaireId);

    record PhotoInfo(UUID id, UUID pieceId, String legende, String contentType, int taille, Instant createdAt) {
    }
}
