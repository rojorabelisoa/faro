package com.faro.affaire;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.faro.common.NotFoundException;
import com.faro.organisation.Organisation;

@Service
@Transactional
public class PieceService {

    private final PieceRepository repository;
    private final AffaireService affaireService;

    public PieceService(PieceRepository repository, AffaireService affaireService) {
        this.repository = repository;
        this.affaireService = affaireService;
    }

    @Transactional(readOnly = true)
    public List<Piece> list(Organisation organisation, UUID affaireId) {
        return repository.findByAffaireIdOrderByOrdreAsc(affaireService.get(organisation, affaireId).getId());
    }

    @Transactional(readOnly = true)
    public Piece get(Organisation organisation, UUID affaireId, UUID id) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        return repository.findByIdAndAffaireId(id, affaire.getId())
            .orElseThrow(() -> new NotFoundException("Pièce introuvable"));
    }

    public Piece create(Organisation organisation, UUID affaireId, PieceRequest request) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        int ordre = request.ordre() != null ? request.ordre()
            : repository.findByAffaireIdOrderByOrdreAsc(affaire.getId()).stream()
                .mapToInt(Piece::getOrdre).max().orElse(0) + 1;
        return repository.save(new Piece(affaire, request.nom(), request.surfaceM2(), ordre));
    }

    public Piece update(Organisation organisation, UUID affaireId, UUID id, PieceRequest request) {
        Piece piece = get(organisation, affaireId, id);
        piece.setNom(request.nom());
        piece.setSurfaceM2(request.surfaceM2());
        if (request.ordre() != null) {
            piece.setOrdre(request.ordre());
        }
        return piece;
    }

    public void delete(Organisation organisation, UUID affaireId, UUID id) {
        repository.delete(get(organisation, affaireId, id));
        // flush : une contrainte violée (lignes de devis liées) remonte ici en 409.
        repository.flush();
    }
}
