package com.faro.bibliotheque;

import java.util.List;

/**
 * Résultat d'un import : rien n'est importé s'il y a au moins une erreur.
 * importes = ouvrages créés ; misAJour = ouvrages existants (même lot, même désignation) mis à jour.
 */
public record ImportResultat(int importes, int misAJour, List<Erreur> erreurs) {

    static ImportResultat erreur(int ligne, String message) {
        return new ImportResultat(0, 0, List.of(new Erreur(ligne, message)));
    }

    public record Erreur(int ligne, String message) {
    }
}
