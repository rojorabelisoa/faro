package com.faro.tableaubord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.faro.affaire.Affaire;
import com.faro.affaire.AffaireService;
import com.faro.affaire.StatutAffaire;
import com.faro.avenant.AvenantService;
import com.faro.devis.DevisService;
import com.faro.marge.MargeResponse;
import com.faro.marge.MargeService;
import com.faro.organisation.Organisation;
import com.faro.tableaubord.TableauDeBordResponse.AFaire;
import com.faro.tableaubord.TableauDeBordResponse.Etape;
import com.faro.tableaubord.TableauDeBordResponse.Indicateur;
import com.faro.tableaubord.TableauDeBordResponse.Mois;
import com.faro.tableaubord.TableauDeBordResponse.Transformation;

@Service
@Transactional(readOnly = true)
public class TableauDeBordService {

    static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    static final Duration RELANCE = Duration.ofDays(14);
    static final List<StatutAffaire> ETAPES = List.of(StatutAffaire.VISITE, StatutAffaire.ESTIMATION,
        StatutAffaire.DOSSIER_TRAVAUX, StatutAffaire.CHANTIER);

    private final JdbcTemplate jdbc;
    private final AffaireService affaireService;
    private final DevisService devisService;
    private final AvenantService avenantService;
    private final MargeService margeService;

    public TableauDeBordService(JdbcTemplate jdbc, AffaireService affaireService, DevisService devisService,
            AvenantService avenantService, MargeService margeService) {
        this.jdbc = jdbc;
        this.affaireService = affaireService;
        this.devisService = devisService;
        this.avenantService = avenantService;
        this.margeService = margeService;
    }

    public TableauDeBordResponse tableau(Organisation organisation, Instant maintenant) {
        YearMonth moisCourant = YearMonth.from(maintenant.atZone(PARIS));
        List<Affaire> affaires = affaireService.list(organisation);

        // Montant signé par mois : dossiers travaux (passage en chantier) + avenants validés.
        Map<YearMonth, BigDecimal> signe = new HashMap<>();
        Instant debut = moisCourant.minusMonths(11).atDay(1).atStartOfDay(PARIS).toInstant();
        cumuler(signe, """
            SELECT to_char(v.created_at AT TIME ZONE 'Europe/Paris', 'YYYY-MM') AS mois, sum(v.total_ht) AS montant
            FROM devis_version v JOIN affaire a ON a.version_chantier_id = v.id
            WHERE a.organisation_id = ? AND v.created_at >= ? GROUP BY 1
            """, organisation.getId(), debut);
        cumuler(signe, """
            SELECT to_char(av.decide_at AT TIME ZONE 'Europe/Paris', 'YYYY-MM') AS mois,
                   sum(round(l.quantite * l.prix_unitaire_ht, 2)) AS montant
            FROM avenant av JOIN affaire a ON a.id = av.affaire_id JOIN ligne_devis l ON l.avenant_id = av.id
            WHERE a.organisation_id = ? AND av.statut = 'VALIDE' AND av.decide_at >= ? GROUP BY 1
            """, organisation.getId(), debut);
        List<Mois> signeParMois = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth mois = moisCourant.minusMonths(i);
            signeParMois.add(new Mois(mois.toString(), signe.getOrDefault(mois, BigDecimal.ZERO)));
        }
        List<BigDecimal> signe6 = signeParMois.subList(6, 12).stream().map(Mois::montantHt).toList();

        // Affaires créées par mois (6 derniers) pour la courbe des affaires actives.
        Map<YearMonth, Integer> creees = new HashMap<>();
        for (Affaire affaire : affaires) {
            creees.merge(YearMonth.from(affaire.getCreatedAt().atZone(PARIS)), 1, Integer::sum);
        }
        List<BigDecimal> creees6 = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            creees6.add(BigDecimal.valueOf(creees.getOrDefault(moisCourant.minusMonths(i), 0)));
        }
        long actives = affaires.stream()
            .filter(a -> a.getStatut() != StatutAffaire.TERMINEE && a.getStatut() != StatutAffaire.PERDUE).count();

        // Pipeline : nombre et montant par étape (devis courant, ou marché actuel en chantier).
        Map<StatutAffaire, int[]> nombres = new EnumMap<>(StatutAffaire.class);
        Map<StatutAffaire, BigDecimal> montants = new EnumMap<>(StatutAffaire.class);
        BigDecimal margeVendu = BigDecimal.ZERO;
        BigDecimal margeReelle = BigDecimal.ZERO;
        BigDecimal enAttente = BigDecimal.ZERO;
        int avenantsEnAttente = 0;
        List<AFaire> aFaire = new ArrayList<>();
        for (Affaire affaire : affaires) {
            StatutAffaire statut = affaire.getStatut();
            if (ETAPES.contains(statut)) {
                nombres.computeIfAbsent(statut, s -> new int[1])[0]++;
                BigDecimal montant = statut == StatutAffaire.CHANTIER
                    ? avenantService.marche(organisation, affaire.getId()).actuelHt()
                    : devisService.devis(organisation, affaire.getId()).totalHt();
                montants.merge(statut, montant, BigDecimal::add);
            }
            if (affaire.getVersionChantierId() != null) {
                MargeResponse marge = margeService.marge(organisation, affaire.getId());
                // Seules les affaires avec des dépenses saisies disent quelque chose de la marge réelle.
                if (marge.total().coutReelHt().signum() > 0) {
                    margeVendu = margeVendu.add(marge.total().venduHt());
                    margeReelle = margeReelle.add(marge.total().margeReelleHt());
                }
                for (var avenant : avenantService.list(organisation, affaire.getId())) {
                    if (avenant.statut() == com.faro.avenant.StatutAvenant.ENVOYE) {
                        enAttente = enAttente.add(avenant.totaux().totalHt());
                        avenantsEnAttente++;
                        aFaire.add(new AFaire("AVENANT_EN_ATTENTE", "Avenant n° " + avenant.numero() + " en attente de réponse",
                            affaire.getId(), affaire.getNom(), avenant.envoyeAt()));
                    }
                }
            }
            if ((statut == StatutAffaire.ESTIMATION || statut == StatutAffaire.DOSSIER_TRAVAUX)
                    && affaire.getUpdatedAt().isBefore(maintenant.minus(RELANCE))) {
                aFaire.add(new AFaire("A_RELANCER", "Sans nouvelle depuis "
                    + Duration.between(affaire.getUpdatedAt(), maintenant).toDays() + " jours : relancer le client",
                    affaire.getId(), affaire.getNom(), affaire.getUpdatedAt()));
            }
        }
        jdbc.query("""
            SELECT a.id, a.nom, max(c.derniere_visite) AS visite
            FROM acces_client c JOIN affaire a ON a.id = c.affaire_id
            WHERE a.organisation_id = ? AND c.revoque_at IS NULL AND c.derniere_visite >= ?
            GROUP BY a.id, a.nom
            """, rs -> {
            aFaire.add(new AFaire("CLIENT_A_CONSULTE", "Le client a consulté son espace", rs.getObject("id", UUID.class),
                rs.getString("nom"), rs.getTimestamp("visite").toInstant()));
        }, organisation.getId(), java.sql.Timestamp.from(maintenant.minus(Duration.ofDays(7))));
        aFaire.sort(Comparator.comparing(AFaire::date, Comparator.nullsLast(Comparator.reverseOrder())));

        List<Etape> pipeline = ETAPES.stream().map(s -> new Etape(s, nombres.getOrDefault(s, new int[1])[0],
            montants.getOrDefault(s, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP))).toList();

        int gagnees = (int) affaires.stream().filter(a -> a.getVersionChantierId() != null).count();
        int perdues = (int) affaires.stream().filter(a -> a.getStatut() == StatutAffaire.PERDUE).count();
        int enCours = (int) affaires.stream().filter(a -> a.getVersionChantierId() == null
            && a.getStatut() != StatutAffaire.PERDUE && a.getStatut() != StatutAffaire.TERMINEE).count();

        BigDecimal signeMois = signe6.get(5);
        return new TableauDeBordResponse(
            new Indicateur(BigDecimal.valueOf(actives), null, creees6,
                creees6.get(5).intValue() + " créée(s) ce mois-ci"),
            new Indicateur(signeMois, variation(signeMois, signe6.get(4)), signe6, "dossiers travaux et avenants"),
            new Indicateur(pourcent(margeReelle, margeVendu), null, List.of(),
                margeVendu.signum() == 0 ? "saisissez les dépenses des chantiers" : "sur les chantiers avec dépenses"),
            new Indicateur(enAttente.setScale(2, RoundingMode.HALF_UP), null, List.of(),
                avenantsEnAttente + " avenant(s) envoyé(s)"),
            new Transformation(gagnees + perdues == 0 ? null
                : pourcent(BigDecimal.valueOf(gagnees), BigDecimal.valueOf(gagnees + perdues)), gagnees, perdues, enCours),
            signeParMois, pipeline, aFaire.stream().limit(6).toList());
    }

    private void cumuler(Map<YearMonth, BigDecimal> cumul, String sql, Object... parametres) {
        Object[] valeurs = parametres.clone();
        for (int i = 0; i < valeurs.length; i++) {
            if (valeurs[i] instanceof Instant instant) {
                valeurs[i] = java.sql.Timestamp.from(instant);
            }
        }
        jdbc.query(sql, rs -> {
            cumul.merge(YearMonth.parse(rs.getString("mois")), rs.getBigDecimal("montant"), BigDecimal::add);
        }, valeurs);
    }

    /** Variation en % (1 décimale), null si la référence est nulle. */
    static BigDecimal variation(BigDecimal valeur, BigDecimal reference) {
        return reference.signum() == 0 ? null
            : valeur.subtract(reference).multiply(BigDecimal.valueOf(100)).divide(reference.abs(), 1, RoundingMode.HALF_UP);
    }

    static BigDecimal pourcent(BigDecimal part, BigDecimal total) {
        return total.signum() == 0 ? null
            : part.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }
}
