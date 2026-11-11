package com.faro.estimation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.faro.affaire.NiveauTravaux;
import com.faro.affaire.TypePiece;
import com.faro.common.NotFoundException;
import com.faro.organisation.Organisation;
import com.faro.organisation.OrganisationCreatedEvent;

@Service
@Transactional
public class RatioService {

    /**
     * Valeurs de départ INDICATIVES (€ HT / m², rafraîchissement / rénovation / rénovation lourde),
     * à remplacer par les prix de l'entreprise : la page Ratios montre les €/m² constatés dans ses devis.
     */
    static final Map<TypePiece, int[][]> RATIOS_PAR_DEFAUT = Map.of(
        TypePiece.CUISINE, new int[][] {{120, 250}, {700, 1300}, {1200, 2000}},
        TypePiece.SALLE_DE_BAINS, new int[][] {{150, 300}, {1200, 2000}, {1800, 3000}},
        TypePiece.WC, new int[][] {{120, 250}, {900, 1600}, {1500, 2500}},
        TypePiece.SEJOUR, new int[][] {{40, 80}, {250, 450}, {500, 900}},
        TypePiece.CHAMBRE, new int[][] {{40, 80}, {250, 450}, {500, 900}},
        TypePiece.ENTREE_DEGAGEMENT, new int[][] {{40, 80}, {200, 400}, {450, 800}},
        TypePiece.BUANDERIE_CELLIER, new int[][] {{50, 100}, {300, 600}, {600, 1000}},
        TypePiece.AUTRE, new int[][] {{40, 90}, {250, 500}, {500, 900}});

    private final RatioRepository repository;
    private final JdbcTemplate jdbc;

    public RatioService(RatioRepository repository, JdbcTemplate jdbc) {
        this.repository = repository;
        this.jdbc = jdbc;
    }

    @EventListener
    public void onOrganisationCreated(OrganisationCreatedEvent event) {
        creerParDefaut(event.organisation());
    }

    /** Ratios de l'organisation (créés si elle date d'avant l'estimation), avec les valeurs constatées. */
    public List<RatioResponse> list(Organisation organisation) {
        Map<String, RatioResponse.Constate> constates = constates(organisation);
        return ratios(organisation).stream()
            .sorted(Comparator.comparing(Ratio::getTypePiece).thenComparing(Ratio::getNiveauTravaux))
            .map(r -> new RatioResponse(r.getId(), r.getTypePiece(), r.getNiveauTravaux(), r.getPrixMinM2(),
                r.getPrixMaxM2(), constates.get(cle(r.getTypePiece(), r.getNiveauTravaux()))))
            .toList();
    }

    public List<Ratio> ratios(Organisation organisation) {
        List<Ratio> ratios = repository.findByOrganisationId(organisation.getId());
        return ratios.isEmpty() ? creerParDefaut(organisation) : ratios;
    }

    public Ratio update(Organisation organisation, UUID id, RatioRequest request) {
        if (request.prixMinM2().compareTo(request.prixMaxM2()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le minimum doit être inférieur ou égal au maximum");
        }
        Ratio ratio = repository.findByIdAndOrganisationId(id, organisation.getId())
            .orElseThrow(() -> new NotFoundException("Ratio introuvable"));
        ratio.setPrixMinM2(request.prixMinM2());
        ratio.setPrixMaxM2(request.prixMaxM2());
        return ratio;
    }

    private List<Ratio> creerParDefaut(Organisation organisation) {
        List<Ratio> ratios = new ArrayList<>();
        RATIOS_PAR_DEFAUT.forEach((type, valeurs) -> {
            NiveauTravaux[] niveaux = NiveauTravaux.values();
            for (int i = 0; i < niveaux.length; i++) {
                ratios.add(new Ratio(organisation, type, niveaux[i], BigDecimal.valueOf(valeurs[i][0]),
                    BigDecimal.valueOf(valeurs[i][1])));
            }
        });
        return repository.saveAll(ratios);
    }

    /** €/m² HT de chaque pièce devisée (typée, mesurée), regroupés par type et niveau. */
    private Map<String, RatioResponse.Constate> constates(Organisation organisation) {
        Map<String, List<BigDecimal>> prixParCle = new HashMap<>();
        jdbc.query("""
            SELECT p.type_piece, p.niveau_travaux, p.surface_m2,
                   SUM(ROUND(l.quantite * l.prix_unitaire_ht, 2)) AS total_ht
            FROM piece p
            JOIN affaire a ON a.id = p.affaire_id
            JOIN ligne_devis l ON l.piece_id = p.id
            WHERE a.organisation_id = ? AND (l.gamme IS NULL OR l.gamme = p.gamme_choisie)
              AND p.type_piece IS NOT NULL AND p.niveau_travaux IS NOT NULL
              AND p.surface_m2 > 0
            GROUP BY p.id, p.type_piece, p.niveau_travaux, p.surface_m2
            HAVING SUM(ROUND(l.quantite * l.prix_unitaire_ht, 2)) > 0
            """, rs -> {
            BigDecimal prixM2 = rs.getBigDecimal("total_ht").divide(rs.getBigDecimal("surface_m2"), 2, RoundingMode.HALF_UP);
            prixParCle.computeIfAbsent(rs.getString("type_piece") + "|" + rs.getString("niveau_travaux"),
                k -> new ArrayList<>()).add(prixM2);
        }, organisation.getId());

        Map<String, RatioResponse.Constate> constates = new HashMap<>();
        prixParCle.forEach((cle, prix) -> {
            prix.sort(null);
            constates.put(cle, new RatioResponse.Constate(prix.size(), prix.get(0), mediane(prix), prix.get(prix.size() - 1)));
        });
        return constates;
    }

    static BigDecimal mediane(List<BigDecimal> tries) {
        int n = tries.size();
        return n % 2 == 1 ? tries.get(n / 2)
            : tries.get(n / 2 - 1).add(tries.get(n / 2)).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private static String cle(TypePiece type, NiveauTravaux niveau) {
        return type.name() + "|" + niveau.name();
    }
}
