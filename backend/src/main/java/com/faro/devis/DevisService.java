package com.faro.devis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.faro.affaire.Affaire;
import com.faro.affaire.AffaireService;
import com.faro.affaire.Piece;
import com.faro.affaire.PieceService;
import com.faro.common.NotFoundException;
import com.faro.devis.DevisResponse.LigneResponse;
import com.faro.devis.DevisResponse.TotalLot;
import com.faro.devis.DevisResponse.TotalPiece;
import com.faro.devis.DevisResponse.TotalTva;
import com.faro.lot.Lot;
import com.faro.lot.LotService;
import com.faro.organisation.Organisation;

@Service
@Transactional
public class DevisService {

    static final String NOM_LIGNES_GENERALES = "Général";

    private final LigneDevisRepository repository;
    private final AffaireService affaireService;
    private final PieceService pieceService;
    private final LotService lotService;

    public DevisService(LigneDevisRepository repository, AffaireService affaireService,
            PieceService pieceService, LotService lotService) {
        this.repository = repository;
        this.affaireService = affaireService;
        this.pieceService = pieceService;
        this.lotService = lotService;
    }

    @Transactional(readOnly = true)
    public DevisResponse devis(Organisation organisation, UUID affaireId) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        List<LigneDevis> lignes = repository.findByAffaireIdOrderByOrdreAsc(affaire.getId());
        List<Piece> pieces = pieceService.list(organisation, affaireId);

        List<TotalPiece> parPiece = new ArrayList<>();
        BigDecimal general = totalHt(lignes.stream().filter(l -> l.getPiece() == null).toList());
        if (general.signum() != 0) {
            parPiece.add(new TotalPiece(null, NOM_LIGNES_GENERALES, general));
        }
        for (Piece piece : pieces) {
            parPiece.add(new TotalPiece(piece.getId(), piece.getNom(), totalHt(lignes.stream()
                .filter(l -> l.getPiece() != null && l.getPiece().getId().equals(piece.getId())).toList())));
        }

        Map<Lot, List<LigneDevis>> lignesParLot = lignes.stream().collect(Collectors.groupingBy(
            LigneDevis::getLot, LinkedHashMap::new, Collectors.toList()));
        List<TotalLot> parLot = lignesParLot.entrySet().stream()
            .sorted(Comparator.comparing((Map.Entry<Lot, List<LigneDevis>> e) -> e.getKey().getOrdre())
                .thenComparing(e -> e.getKey().getCode()))
            .map(e -> new TotalLot(e.getKey().getId(), e.getKey().getCode(), e.getKey().getNom(), totalHt(e.getValue())))
            .toList();

        Map<BigDecimal, BigDecimal> baseParTaux = new TreeMap<>();
        for (LigneDevis ligne : lignes) {
            baseParTaux.merge(ligne.getTauxTva(), ligne.getMontantHt(), BigDecimal::add);
        }
        List<TotalTva> tva = baseParTaux.entrySet().stream()
            .map(e -> new TotalTva(e.getKey(), e.getValue(), Montants.tva(e.getValue(), e.getKey())))
            .toList();

        BigDecimal totalHt = totalHt(lignes);
        BigDecimal totalTva = tva.stream().map(TotalTva::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DevisResponse(lignes.stream().map(LigneResponse::from).toList(), parPiece, parLot, tva,
            totalHt, Montants.arrondi(totalTva), Montants.arrondi(totalHt.add(totalTva)));
    }

    public LigneDevis create(Organisation organisation, UUID affaireId, LigneDevisRequest request) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        int ordre = request.ordre() != null ? request.ordre()
            : repository.findByAffaireIdOrderByOrdreAsc(affaire.getId()).stream()
                .mapToInt(LigneDevis::getOrdre).max().orElse(0) + 1;
        LigneDevis ligne = new LigneDevis(affaire, ordre);
        apply(organisation, affaireId, ligne, request);
        return repository.save(ligne);
    }

    public LigneDevis update(Organisation organisation, UUID affaireId, UUID id, LigneDevisRequest request) {
        LigneDevis ligne = get(organisation, affaireId, id);
        apply(organisation, affaireId, ligne, request);
        if (request.ordre() != null) {
            ligne.setOrdre(request.ordre());
        }
        return ligne;
    }

    public void delete(Organisation organisation, UUID affaireId, UUID id) {
        repository.delete(get(organisation, affaireId, id));
    }

    private LigneDevis get(Organisation organisation, UUID affaireId, UUID id) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        return repository.findByIdAndAffaireId(id, affaire.getId())
            .orElseThrow(() -> new NotFoundException("Ligne de devis introuvable"));
    }

    private void apply(Organisation organisation, UUID affaireId, LigneDevis ligne, LigneDevisRequest request) {
        BigDecimal tauxTva = Objects.requireNonNullElse(request.tauxTva(), Montants.TAUX_TVA_PAR_DEFAUT);
        if (!Montants.tauxValide(tauxTva)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Taux de TVA invalide : " + tauxTva + " (autorisés : 0, 5.5, 10, 20)");
        }
        ligne.setPiece(request.pieceId() != null ? pieceService.get(organisation, affaireId, request.pieceId()) : null);
        ligne.setLot(lotService.get(organisation, request.lotId()));
        ligne.setDesignation(request.designation());
        ligne.setDescription(request.description());
        ligne.setUnite(request.unite());
        ligne.setQuantite(request.quantite());
        ligne.setPrixUnitaireHt(request.prixUnitaireHt());
        ligne.setTauxTva(tauxTva.setScale(2, RoundingMode.HALF_UP));
    }

    private static BigDecimal totalHt(List<LigneDevis> lignes) {
        return Montants.arrondi(lignes.stream().map(LigneDevis::getMontantHt).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
