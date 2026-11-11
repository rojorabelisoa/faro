package com.faro.version;

import com.faro.devis.DevisResponse;

/** Ce qui est figé dans une version : le devis complet et l'en-tête tel qu'il était. */
public record VersionContenu(String entreprise, String affaire, String client, String adresse, DevisResponse devis) {
}
