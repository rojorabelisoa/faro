package com.faro.bibliotheque;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.faro.common.Montants;
import com.faro.lot.Lot;
import com.faro.lot.LotService;
import com.faro.organisation.Organisation;

/**
 * Import / export CSV de la bibliothèque d'ouvrages.
 * Colonnes : lot (code), designation, unite, prix_unitaire_ht, tva (facultative, 10 par défaut), description (facultative).
 */
@Service
@Transactional
public class OuvrageCsvService {

    static final List<String> COLONNES = List.of("lot", "designation", "unite", "prix_unitaire_ht", "tva", "description");
    private static final List<String> OBLIGATOIRES = List.of("lot", "designation", "unite", "prix_unitaire_ht");
    private static final int MAX_LIGNES = 5000;

    private final OuvrageRepository repository;
    private final LotService lotService;

    public OuvrageCsvService(OuvrageRepository repository, LotService lotService) {
        this.repository = repository;
        this.lotService = lotService;
    }

    @Transactional(readOnly = true)
    public byte[] export(Organisation organisation) {
        Map<java.util.UUID, String> codes = lotService.list(organisation).stream()
            .collect(Collectors.toMap(Lot::getId, Lot::getCode));
        StringBuilder csv = new StringBuilder("﻿").append(Csv.ligne(COLONNES));
        for (Ouvrage ouvrage : repository.search(organisation.getId(), null, null)) {
            csv.append(Csv.ligne(List.of(codes.get(ouvrage.getLot().getId()), ouvrage.getDesignation(),
                ouvrage.getUnite(), decimal(ouvrage.getPrixUnitaireHt()), decimal(ouvrage.getTauxTva()),
                ouvrage.getDescription() == null ? "" : ouvrage.getDescription())));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public ImportResultat importer(Organisation organisation, byte[] contenu) {
        List<List<String>> lignes = Csv.parse(Csv.decode(contenu));
        List<ImportResultat.Erreur> erreurs = new ArrayList<>();
        if (lignes.isEmpty()) {
            return ImportResultat.erreur(1, "Fichier vide");
        }
        if (lignes.size() - 1 > MAX_LIGNES) {
            return ImportResultat.erreur(1, "Trop de lignes (" + (lignes.size() - 1) + "), maximum " + MAX_LIGNES);
        }

        Map<String, Integer> index = new HashMap<>();
        List<String> entete = lignes.get(0);
        for (int i = 0; i < entete.size(); i++) {
            index.put(normaliser(entete.get(i)), i);
        }
        List<String> manquantes = OBLIGATOIRES.stream().filter(c -> !index.containsKey(c)).toList();
        if (!manquantes.isEmpty()) {
            return ImportResultat.erreur(1,
                "Colonnes manquantes : " + String.join(", ", manquantes) + " (attendues : " + String.join(";", COLONNES) + ")");
        }

        Map<String, Lot> lots = lotService.list(organisation).stream()
            .collect(Collectors.toMap(lot -> lot.getCode().toLowerCase(Locale.ROOT), Function.identity()));
        // Réimporter un fichier met à jour les prix : un ouvrage est reconnu par son lot et sa désignation.
        Map<String, Ouvrage> existants = new HashMap<>();
        for (Ouvrage ouvrage : repository.search(organisation.getId(), null, null)) {
            existants.putIfAbsent(cle(ouvrage.getLot(), ouvrage.getDesignation()), ouvrage);
        }
        List<Ouvrage> ouvrages = new ArrayList<>();
        int misAJour = 0;
        for (int n = 1; n < lignes.size(); n++) {
            List<String> ligne = lignes.get(n);
            Function<String, String> champ = colonne -> {
                Integer i = index.get(colonne);
                return i == null || i >= ligne.size() ? "" : ligne.get(i);
            };
            int numero = n + 1;
            try {
                Lot lot = lots.get(champ.apply("lot").toLowerCase(Locale.ROOT));
                if (lot == null) {
                    throw new IllegalArgumentException("lot inconnu « " + champ.apply("lot") + " » (code de lot attendu, ex. 07)");
                }
                String designation = obligatoire(champ.apply("designation"), "désignation", 500);
                String unite = obligatoire(champ.apply("unite"), "unité", 20);
                BigDecimal prix = nombre(champ.apply("prix_unitaire_ht"), "prix unitaire HT");
                String tva = champ.apply("tva");
                BigDecimal taux;
                try {
                    taux = Montants.tauxTva(tva.isEmpty() ? null : nombre(tva.replace("%", ""), "TVA"));
                } catch (ResponseStatusException e) {
                    throw new IllegalArgumentException("TVA invalide « " + tva + " » (0 ; 5,5 ; 10 ou 20)");
                }
                Ouvrage existant = existants.get(cle(lot, designation));
                Ouvrage ouvrage = existant != null ? existant : new Ouvrage(organisation);
                if (existant != null) {
                    misAJour++;
                } else {
                    existants.put(cle(lot, designation), ouvrage);
                }
                ouvrage.setLot(lot);
                ouvrage.setDesignation(designation);
                ouvrage.setUnite(unite);
                ouvrage.setPrixUnitaireHt(prix);
                ouvrage.setTauxTva(taux);
                String description = champ.apply("description");
                ouvrage.setDescription(description.isEmpty() ? null : description);
                ouvrages.add(ouvrage);
            } catch (IllegalArgumentException e) {
                erreurs.add(new ImportResultat.Erreur(numero, e.getMessage()));
            }
        }
        if (!erreurs.isEmpty()) {
            // Annule aussi les modifications faites sur les ouvrages existants.
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus()
                .setRollbackOnly();
            return new ImportResultat(0, 0, erreurs);
        }
        repository.saveAll(ouvrages);
        return new ImportResultat(ouvrages.size() - misAJour, misAJour, List.of());
    }

    private static String cle(Lot lot, String designation) {
        return lot.getId() + "|" + designation.trim().toLowerCase(Locale.ROOT);
    }

    /** « Prix unitaire HT » → « prix_unitaire_ht » ; « Désignation » → « designation ». */
    static String normaliser(String entete) {
        String sansAccents = Normalizer.normalize(entete, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String cle = sansAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        return switch (cle) {
            case "code_lot", "lot_code" -> "lot";
            case "pu_ht", "prix_ht", "prix", "pu" -> "prix_unitaire_ht";
            case "taux_tva", "tva_percent" -> "tva";
            case "descriptif" -> "description";
            default -> cle;
        };
    }

    private static String obligatoire(String valeur, String nom, int max) {
        if (valeur.isEmpty()) {
            throw new IllegalArgumentException(nom + " manquante");
        }
        if (valeur.length() > max) {
            throw new IllegalArgumentException(nom + " trop longue (" + max + " caractères maximum)");
        }
        return valeur;
    }

    /** Accepte « 1 234,50 », « 1234.5 », « 72,40 € ». */
    static BigDecimal nombre(String valeur, String nom) {
        String nettoye = valeur.replaceAll("[\\s  €]", "").replace(',', '.');
        try {
            return new BigDecimal(nettoye).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nom + " invalide « " + valeur + " »");
        }
    }

    private static String decimal(BigDecimal valeur) {
        return valeur.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
