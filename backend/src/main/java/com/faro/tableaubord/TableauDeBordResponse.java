package com.faro.tableaubord;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.faro.affaire.StatutAffaire;

/**
 * Indicateurs de l'entreprise pour la page d'accueil. Montants HT.
 * montants = false : version réduite pour le conducteur de travaux (voir {@link #sansMontants()}).
 */
public record TableauDeBordResponse(
        Indicateur affairesActives,
        Indicateur signeCeMois,
        Indicateur margeReelle,
        Indicateur enAttenteClient,
        Transformation transformation,
        List<Mois> signeParMois,
        List<Etape> pipeline,
        List<AFaire> aFaire,
        boolean montants) {

    /**
     * Sans rien de financier : seuls restent le nombre d'affaires actives, le nombre d'affaires par étape et la
     * liste « À faire » ; les indicateurs financiers sont null et le signé par mois est vide.
     */
    public TableauDeBordResponse sansMontants() {
        return new TableauDeBordResponse(affairesActives, null, null, null, null, List.of(),
            pipeline.stream().map(e -> new Etape(e.statut(), e.nombre(), null)).toList(), aFaire, false);
    }

    /**
     * valeur : le chiffre affiché ; tendance : variation en % par rapport au mois précédent (null si non calculable) ;
     * serie : 6 derniers mois pour la mini-courbe (vide si sans objet) ; detail : texte d'appoint.
     */
    public record Indicateur(BigDecimal valeur, BigDecimal tendance, List<BigDecimal> serie, String detail) {
    }

    /** Affaires gagnées (passées en chantier) contre perdues ; taux en % (null si aucune décision). */
    public record Transformation(BigDecimal taux, int gagnees, int perdues, int enCours) {
    }

    /** mois au format AAAA-MM. */
    public record Mois(String mois, BigDecimal montantHt) {
    }

    public record Etape(StatutAffaire statut, int nombre, BigDecimal montantHt) {
    }

    /** type : AVENANT_EN_ATTENTE, A_RELANCER, CLIENT_A_CONSULTE. */
    public record AFaire(String type, String libelle, UUID affaireId, String affaire, Instant date) {
    }
}
