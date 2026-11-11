package com.faro.estimation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.faro.affaire.Piece;
import com.faro.affaire.PieceService;
import com.faro.common.Montants;
import com.faro.devis.DevisService;
import com.faro.organisation.Organisation;

@Service
@Transactional
public class EstimationService {

    private final PieceService pieceService;
    private final RatioService ratioService;
    private final DevisService devisService;

    public EstimationService(PieceService pieceService, RatioService ratioService, DevisService devisService) {
        this.pieceService = pieceService;
        this.ratioService = ratioService;
        this.devisService = devisService;
    }

    public EstimationResponse estimation(Organisation organisation, UUID affaireId) {
        Map<String, Ratio> ratios = ratioService.ratios(organisation).stream()
            .collect(Collectors.toMap(r -> r.getTypePiece() + "|" + r.getNiveauTravaux(), r -> r));
        List<EstimationResponse.PieceEstimee> estimees = new ArrayList<>();
        BigDecimal totalMin = BigDecimal.ZERO;
        BigDecimal totalMax = BigDecimal.ZERO;
        for (Piece piece : pieceService.list(organisation, affaireId)) {
            String manque = piece.getSurfaceM2() == null || piece.getSurfaceM2().signum() == 0 ? "surface"
                : piece.getTypePiece() == null ? "type de pièce"
                : piece.getNiveauTravaux() == null ? "niveau de travaux" : null;
            BigDecimal min = null;
            BigDecimal max = null;
            if (manque == null) {
                Ratio ratio = ratios.get(piece.getTypePiece() + "|" + piece.getNiveauTravaux());
                min = Montants.arrondi(piece.getSurfaceM2().multiply(ratio.getPrixMinM2()));
                max = Montants.arrondi(piece.getSurfaceM2().multiply(ratio.getPrixMaxM2()));
                totalMin = totalMin.add(min);
                totalMax = totalMax.add(max);
            }
            estimees.add(new EstimationResponse.PieceEstimee(piece.getId(), piece.getNom(), piece.getTypePiece(),
                piece.getNiveauTravaux(), piece.getSurfaceM2(), min, max, manque));
        }
        return new EstimationResponse(estimees, Montants.arrondi(totalMin), Montants.arrondi(totalMax),
            devisService.devis(organisation, affaireId).totalHt());
    }
}
