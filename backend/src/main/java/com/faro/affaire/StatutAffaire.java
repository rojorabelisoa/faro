package com.faro.affaire;

/** Étapes d'une affaire, de la première visite à la fin du chantier. */
public enum StatutAffaire {
    VISITE,
    ESTIMATION,
    DOSSIER_TRAVAUX,
    CHANTIER,
    TERMINEE,
    PERDUE
}
