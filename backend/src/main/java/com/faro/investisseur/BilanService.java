package com.faro.investisseur;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.faro.affaire.Affaire;
import com.faro.affaire.AffaireService;
import com.faro.avenant.AvenantService;
import com.faro.investisseur.BilanDtos.BilanRequest;
import com.faro.investisseur.BilanDtos.BilanResponse;
import com.faro.investisseur.BilanDtos.Indicateurs;
import com.faro.organisation.Organisation;

/**
 * Bilan d'opération pour investisseur / marchand de biens. Les travaux valent le montant actuel du marché TTC
 * (devis, ou dossier travaux + avenants validés). TRI annualisé simple : un décaissement au départ, l'encaissement
 * de la revente à la fin, soit (revente / coût total)^(12 / durée) − 1 (sans financement ni flux intermédiaires).
 */
@Service
@Transactional
public class BilanService {

    static final BigDecimal SURCOUT_TRAVAUX = new BigDecimal("1.10");
    static final int RETARD_MOIS = 3;

    private final BilanOperationRepository repository;
    private final AffaireService affaireService;
    private final AvenantService avenantService;

    public BilanService(BilanOperationRepository repository, AffaireService affaireService,
            AvenantService avenantService) {
        this.repository = repository;
        this.affaireService = affaireService;
        this.avenantService = avenantService;
    }

    @Transactional(readOnly = true)
    public BilanResponse bilan(Organisation organisation, UUID affaireId) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        BigDecimal travaux = avenantService.marche(organisation, affaireId).actuelTtc();
        return repository.findById(affaire.getId())
            .map(b -> new BilanResponse(new BilanRequest(b.getPrixAchat(), b.getFraisAcquisition(), b.getAutresFrais(),
                b.getPrixRevente(), b.getDureeMois()), travaux, scenarios(b, travaux)))
            .orElse(new BilanResponse(null, travaux, List.of()));
    }

    public BilanResponse enregistrer(Organisation organisation, UUID affaireId, BilanRequest request) {
        Affaire affaire = affaireService.get(organisation, affaireId);
        BilanOperation bilan = repository.findById(affaire.getId()).orElseGet(() -> new BilanOperation(affaire.getId()));
        bilan.setPrixAchat(request.prixAchat());
        bilan.setFraisAcquisition(request.fraisAcquisition());
        bilan.setAutresFrais(request.autresFrais());
        bilan.setPrixRevente(request.prixRevente());
        bilan.setDureeMois(request.dureeMois());
        repository.save(bilan);
        return bilan(organisation, affaireId);
    }

    static List<Indicateurs> scenarios(BilanOperation b, BigDecimal travaux) {
        return List.of(
            indicateurs("Prévu", b, travaux, b.getDureeMois()),
            indicateurs("Travaux +10 %", b, travaux.multiply(SURCOUT_TRAVAUX), b.getDureeMois()),
            indicateurs("Retard de " + RETARD_MOIS + " mois", b, travaux, b.getDureeMois() + RETARD_MOIS));
    }

    static Indicateurs indicateurs(String nom, BilanOperation b, BigDecimal travaux, int duree) {
        BigDecimal cout = b.getPrixAchat().add(b.getFraisAcquisition()).add(travaux).add(b.getAutresFrais());
        BigDecimal marge = b.getPrixRevente().subtract(cout);
        BigDecimal rentabilite = cout.signum() == 0 ? null
            : marge.multiply(BigDecimal.valueOf(100)).divide(cout, 1, RoundingMode.HALF_UP);
        BigDecimal tri = null;
        if (cout.signum() > 0 && b.getPrixRevente().signum() > 0) {
            double multiple = b.getPrixRevente().divide(cout, MathContext.DECIMAL64).doubleValue();
            tri = BigDecimal.valueOf((Math.pow(multiple, 12.0 / duree) - 1) * 100).setScale(1, RoundingMode.HALF_UP);
        }
        return new Indicateurs(nom, travaux.setScale(2, RoundingMode.HALF_UP), cout.setScale(2, RoundingMode.HALF_UP),
            marge.setScale(2, RoundingMode.HALF_UP), rentabilite, tri, duree);
    }
}
