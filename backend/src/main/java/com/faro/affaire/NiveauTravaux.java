package com.faro.affaire;

/** Ampleur des travaux dans une pièce, pour l'estimation rapide. */
public enum NiveauTravaux {
    /** Peinture, sols, petits travaux. */
    RAFRAICHISSEMENT,
    /** Remise à neuf complète sans toucher à la structure. */
    RENOVATION,
    /** Redistribution, réseaux refaits, démolition importante. */
    RENOVATION_LOURDE
}
