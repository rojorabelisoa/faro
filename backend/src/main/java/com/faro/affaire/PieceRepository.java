package com.faro.affaire;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PieceRepository extends JpaRepository<Piece, UUID> {

    List<Piece> findByAffaireIdOrderByOrdreAsc(UUID affaireId);

    Optional<Piece> findByIdAndAffaireId(UUID id, UUID affaireId);
}
