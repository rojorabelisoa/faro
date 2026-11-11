package com.faro.devis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.faro.avenant.Avenant;
import com.faro.avenant.StatutAvenant;
import com.faro.bibliotheque.Ouvrage;
import com.faro.bibliotheque.OuvrageService;
import com.faro.common.Montants;
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
    private final OuvrageService ouvrageService;

    public DevisService(LigneDevisRepository repository, AffaireService affaireService,
            PieceService pieceService, LotService lotService, OuvrageService ouvrageService) {
        this.repository = repository;
        this.affaireService = affaireService;
        this.pieceService = pieceService;
        this.lotService = lotService;
        this.ouvrageService = ouvrageService;
    }

    @Transactional(readOnly = true)
    public DevisResponse devis(Organisation organisation, UUID affaireId) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        List<LigneDevis> lignes = repository.findByAffaireIdAndAvenantIsNullOrderByOrdreAsc(affaire.getId());
        List<Piece> pieces = pieceService.list(organisation, affaireId);
        // Les totaux ne comptent que les lignes communes et celles de la gamme retenue.
        List<LigneDevis> incluses = lignes.stream().filter(LigneDevis::isIncluse).toList();

        List<TotalPiece> parPiece = new ArrayList<>();
        List<LigneDevis> generales = lignes.stream().filter(l -> l.getPiece() == null).toList();
        if (!generales.isEmpty()) {
            parPiece.add(new TotalPiece(null, NOM_LIGNES_GENERALES,
                totalHt(generales.stream().filter(LigneDevis::isIncluse).toList()),
                affaire.getGammeChoisie(), totauxParGamme(generales)));
        }
        for (Piece piece : pieces) {
            List<LigneDevis> lignesPiece = lignes.stream()
                .filter(l -> l.getPiece() != null && l.getPiece().getId().equals(piece.getId())).toList();
            parPiece.add(new TotalPiece(piece.getId(), piece.getNom(),
                totalHt(lignesPiece.stream().filter(LigneDevis::isIncluse).toList()),
                piece.getGammeChoisie(), totauxParGamme(lignesPiece)));
        }

        Map<Lot, List<LigneDevis>> lignesParLot = incluses.stream().collect(Collectors.groupingBy(
            LigneDevis::getLot, LinkedHashMap::new, Collectors.toList()));
        List<TotalLot> parLot = lignesParLot.entrySet().stream()
            .sorted(Comparator.comparing((Map.Entry<Lot, List<LigneDevis>> e) -> e.getKey().getOrdre())
                .thenComparing(e -> e.getKey().getCode()))
            .map(e -> new TotalLot(e.getKey().getId(), e.getKey().getCode(), e.getKey().getNom(), totalHt(e.getValue())))
            .toList();

        Map<BigDecimal, BigDecimal> baseParTaux = new TreeMap<>();
        for (LigneDevis ligne : incluses) {
            baseParTaux.merge(ligne.getTauxTva(), ligne.getMontantHt(), BigDecimal::add);
        }
        List<TotalTva> tva = baseParTaux.entrySet().stream()
            .map(e -> new TotalTva(e.getKey(), e.getValue(), Montants.tva(e.getValue(), e.getKey())))
            .toList();

        BigDecimal totalHt = totalHt(incluses);
        BigDecimal totalTva = tva.stream().map(TotalTva::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DevisResponse(lignes.stream().map(LigneResponse::from).toList(), parPiece, parLot, tva,
            totalHt, Montants.arrondi(totalTva), Montants.arrondi(totalHt.add(totalTva)), totauxParGamme(lignes));
    }

    /**
     * Total HT des lignes si la gamme g était retenue partout (lignes communes + lignes de g),
     * pour comparer les options. Null s'il n'y a aucune ligne d'option.
     */
    static Map<Gamme, BigDecimal> totauxParGamme(List<LigneDevis> lignes) {
        if (lignes.stream().noneMatch(l -> l.getGamme() != null)) {
            return null;
        }
        Map<Gamme, BigDecimal> totaux = new java.util.EnumMap<>(Gamme.class);
        for (Gamme gamme : Gamme.values()) {
            totaux.put(gamme, totalHt(lignes.stream().filter(l -> l.getGamme() == null || l.getGamme() == gamme).toList()));
        }
        return totaux;
    }

    /** Gamme retenue pour une pièce, ou pour les lignes générales de l'affaire si pieceId est null. */
    public void choisirGamme(Organisation organisation, UUID affaireId, UUID pieceId, Gamme gamme) {
        if (pieceId == null) {
            affaireService.get(organisation, affaireId).setGammeChoisie(gamme);
        } else {
            pieceService.get(organisation, affaireId, pieceId).setGammeChoisie(gamme);
        }
    }

    public LigneDevis create(Organisation organisation, UUID affaireId, LigneDevisRequest request) {
        verifierDevisModifiable(affaireService.get(organisation, affaireId));
        LigneDevis ligne = newLigne(organisation, affaireId, request.ordre());
        apply(organisation, affaireId, ligne, request);
        return repository.save(ligne);
    }

    /** Ajoute une ligne en copiant un ouvrage de la bibliothèque (modifiable ensuite comme toute ligne). */
    public LigneDevis createFromOuvrage(Organisation organisation, UUID affaireId, LigneDepuisOuvrageRequest request) {
        verifierDevisModifiable(affaireService.get(organisation, affaireId));
        Ouvrage ouvrage = ouvrageService.get(organisation, request.ouvrageId());
        LigneDevis ligne = newLigne(organisation, affaireId, null);
        ligne.setOuvrage(ouvrage);
        ligne.setPiece(piece(organisation, affaireId, request.pieceId()));
        ligne.setLot(ouvrage.getLot());
        ligne.setDesignation(ouvrage.getDesignation());
        ligne.setDescription(ouvrage.getDescription());
        ligne.setUnite(ouvrage.getUnite());
        ligne.setQuantite(request.quantite());
        ligne.setPrixUnitaireHt(ouvrage.getPrixUnitaireHt());
        ligne.setTauxTva(ouvrage.getTauxTva());
        ligne.setDebourseUnitaireHt(ouvrage.getDebourseUnitaireHt());
        return repository.save(ligne);
    }

    public LigneDevis update(Organisation organisation, UUID affaireId, UUID id, LigneDevisRequest request) {
        LigneDevis ligne = get(organisation, affaireId, id);
        verifierModifiable(ligne);
        apply(organisation, affaireId, ligne, request);
        if (request.ordre() != null) {
            ligne.setOrdre(request.ordre());
        }
        return ligne;
    }

    /** Réordonne les lignes données (toutes doivent appartenir à l'affaire) ; les autres gardent leur ordre. */
    public void reorder(Organisation organisation, UUID affaireId, List<UUID> ids) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        verifierDevisModifiable(affaire);
        Map<UUID, LigneDevis> lignes = repository.findByAffaireIdAndAvenantIsNullOrderByOrdreAsc(affaire.getId()).stream()
            .collect(Collectors.toMap(LigneDevis::getId, l -> l));
        for (int i = 0; i < ids.size(); i++) {
            LigneDevis ligne = lignes.get(ids.get(i));
            if (ligne == null) {
                throw new NotFoundException("Ligne de devis introuvable : " + ids.get(i));
            }
            ligne.setOrdre(i + 1);
        }
    }

    public void delete(Organisation organisation, UUID affaireId, UUID id) {
        LigneDevis ligne = get(organisation, affaireId, id);
        verifierModifiable(ligne);
        repository.delete(ligne);
    }

    /** Ligne d'avenant : l'avenant (en brouillon) est vérifié par l'appelant. */
    public LigneDevis createForAvenant(Organisation organisation, UUID affaireId, Avenant avenant,
            LigneDevisRequest request) {
        int ordre = repository.findByAvenantIdOrderByOrdreAsc(avenant.getId()).stream()
            .mapToInt(LigneDevis::getOrdre).max().orElse(0) + 1;
        LigneDevis ligne = new LigneDevis(affaireService.get(organisation, affaireId), ordre);
        ligne.setAvenant(avenant);
        apply(organisation, affaireId, ligne, request);
        return repository.save(ligne);
    }

    /** Une fois en chantier, le devis de base est figé : tout changement passe par un avenant. */
    static void verifierDevisModifiable(Affaire affaire) {
        if (affaire.getVersionChantierId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "L'affaire est en chantier : le devis est figé, passez par un avenant.");
        }
    }

    private static void verifierModifiable(LigneDevis ligne) {
        if (ligne.getAvenant() == null) {
            verifierDevisModifiable(ligne.getAffaire());
        } else if (ligne.getAvenant().getStatut() != StatutAvenant.BROUILLON) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "L'avenant a été envoyé au client : ses lignes ne sont plus modifiables.");
        }
    }

    private LigneDevis newLigne(Organisation organisation, UUID affaireId, Integer ordre) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        int position = ordre != null ? ordre
            : repository.findByAffaireIdAndAvenantIsNullOrderByOrdreAsc(affaire.getId()).stream()
                .mapToInt(LigneDevis::getOrdre).max().orElse(0) + 1;
        return new LigneDevis(affaire, position);
    }

    private Piece piece(Organisation organisation, UUID affaireId, UUID pieceId) {
        return pieceId != null ? pieceService.get(organisation, affaireId, pieceId) : null;
    }

    private LigneDevis get(Organisation organisation, UUID affaireId, UUID id) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        return repository.findByIdAndAffaireId(id, affaire.getId())
            .orElseThrow(() -> new NotFoundException("Ligne de devis introuvable"));
    }

    private void apply(Organisation organisation, UUID affaireId, LigneDevis ligne, LigneDevisRequest request) {
        ligne.setTauxTva(Montants.tauxTva(request.tauxTva()));
        ligne.setGamme(request.gamme());
        ligne.setDebourseUnitaireHt(request.debourseUnitaireHt());
        ligne.setPiece(piece(organisation, affaireId, request.pieceId()));
        ligne.setLot(lotService.get(organisation, request.lotId()));
        ligne.setDesignation(request.designation());
        ligne.setDescription(request.description());
        ligne.setUnite(request.unite());
        ligne.setQuantite(request.quantite());
        ligne.setPrixUnitaireHt(request.prixUnitaireHt());
    }

    private static BigDecimal totalHt(List<LigneDevis> lignes) {
        return Montants.arrondi(lignes.stream().map(LigneDevis::getMontantHt).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
