package com.faro.marge;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.faro.affaire.Affaire;
import com.faro.affaire.AffaireService;
import com.faro.avenant.StatutAvenant;
import com.faro.common.Montants;
import com.faro.common.NotFoundException;
import com.faro.devis.LigneDevis;
import com.faro.devis.LigneDevisRepository;
import com.faro.lot.Lot;
import com.faro.lot.LotService;
import com.faro.organisation.Organisation;

@Service
@Transactional
public class MargeService {

    private final DepenseRepository depenses;
    private final LigneDevisRepository lignes;
    private final AffaireService affaireService;
    private final LotService lotService;

    public MargeService(DepenseRepository depenses, LigneDevisRepository lignes, AffaireService affaireService,
            LotService lotService) {
        this.depenses = depenses;
        this.lignes = lignes;
        this.affaireService = affaireService;
        this.lotService = lotService;
    }

    @Transactional(readOnly = true)
    public MargeResponse marge(Organisation organisation, UUID affaireId) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        // Vendu : lignes retenues du devis de base + lignes des avenants validés.
        List<LigneDevis> vendues = lignes.findByAffaireIdAndAvenantIsNullOrderByOrdreAsc(affaire.getId()).stream()
            .filter(LigneDevis::isIncluse).toList();
        List<LigneDevis> avenants = lignesAvenantsValides(affaire);
        List<LigneDevis> toutes = Stream.concat(vendues.stream(), avenants.stream()).toList();
        List<Depense> reelles = depenses.findByAffaireIdOrderByDateDepenseAscCreatedAtAsc(affaire.getId());

        Map<UUID, Accumulateur> parLot = new LinkedHashMap<>();
        for (LigneDevis ligne : toutes) {
            Accumulateur acc = parLot.computeIfAbsent(ligne.getLot().getId(), id -> new Accumulateur(ligne.getLot()));
            acc.vendu = acc.vendu.add(ligne.getMontantHt());
            if (ligne.getDebourseUnitaireHt() != null) {
                acc.prevu = acc.prevu.add(Montants.montantHt(ligne.getQuantite(), ligne.getDebourseUnitaireHt()));
            } else if (ligne.getMontantHt().signum() != 0) {
                acc.sansDebourse++;
            }
        }
        for (Depense depense : reelles) {
            Accumulateur acc = parLot.computeIfAbsent(depense.getLot().getId(), id -> new Accumulateur(depense.getLot()));
            acc.reel = acc.reel.add(depense.getMontantHt());
        }

        List<MargeResponse.Lot> lots = new ArrayList<>();
        BigDecimal vendu = BigDecimal.ZERO;
        BigDecimal prevu = BigDecimal.ZERO;
        BigDecimal reel = BigDecimal.ZERO;
        int sansDebourse = 0;
        for (Accumulateur acc : parLot.values().stream()
                .sorted(Comparator.comparing((Accumulateur a) -> a.lot.getOrdre()).thenComparing(a -> a.lot.getCode()))
                .toList()) {
            lots.add(new MargeResponse.Lot(acc.lot.getId(), acc.lot.getCode(), acc.lot.getNom(), acc.vendu, acc.prevu,
                acc.reel, acc.vendu.subtract(acc.prevu), acc.vendu.subtract(acc.reel), acc.sansDebourse));
            vendu = vendu.add(acc.vendu);
            prevu = prevu.add(acc.prevu);
            reel = reel.add(acc.reel);
            sansDebourse += acc.sansDebourse;
        }
        BigDecimal margePrevue = vendu.subtract(prevu);
        BigDecimal margeReelle = vendu.subtract(reel);
        return new MargeResponse(lots, new MargeResponse.Total(vendu, prevu, reel, margePrevue, margeReelle,
            taux(margePrevue, vendu), taux(margeReelle, vendu), sansDebourse),
            reelles.stream().map(DepenseResponse::from).toList());
    }

    private List<LigneDevis> lignesAvenantsValides(Affaire affaire) {
        return lignes.findByAffaireIdAndAvenantIsNotNull(affaire.getId()).stream()
            .filter(l -> l.getAvenant().getStatut() == StatutAvenant.VALIDE).toList();
    }

    /** Taux de marge en % du vendu HT (null si rien n'est vendu). */
    static BigDecimal taux(BigDecimal marge, BigDecimal vendu) {
        return vendu.signum() == 0 ? null
            : marge.multiply(BigDecimal.valueOf(100)).divide(vendu, 1, RoundingMode.HALF_UP);
    }

    public Depense addDepense(Organisation organisation, UUID affaireId, DepenseRequest request) {
        Depense depense = new Depense(affaireService.get(organisation, affaireId));
        apply(organisation, depense, request);
        return depenses.save(depense);
    }

    public Depense updateDepense(Organisation organisation, UUID affaireId, UUID id, DepenseRequest request) {
        Depense depense = depense(organisation, affaireId, id);
        apply(organisation, depense, request);
        return depense;
    }

    public void deleteDepense(Organisation organisation, UUID affaireId, UUID id) {
        depenses.delete(depense(organisation, affaireId, id));
    }

    private Depense depense(Organisation organisation, UUID affaireId, UUID id) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        return depenses.findByIdAndAffaireId(id, affaire.getId())
            .orElseThrow(() -> new NotFoundException("Dépense introuvable"));
    }

    private void apply(Organisation organisation, Depense depense, DepenseRequest request) {
        depense.setLot(lotService.get(organisation, request.lotId()));
        depense.setNature(request.nature());
        depense.setLibelle(request.libelle().trim());
        depense.setFournisseur(request.fournisseur() == null || request.fournisseur().isBlank() ? null
            : request.fournisseur().trim());
        depense.setMontantHt(request.montantHt());
        depense.setDateDepense(request.dateDepense() != null ? request.dateDepense() : LocalDate.now());
    }

    private static final class Accumulateur {
        final Lot lot;
        BigDecimal vendu = BigDecimal.ZERO;
        BigDecimal prevu = BigDecimal.ZERO;
        BigDecimal reel = BigDecimal.ZERO;
        int sansDebourse;

        Accumulateur(Lot lot) {
            this.lot = lot;
        }
    }
}
