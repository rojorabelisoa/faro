package com.faro.devis;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.faro.devis.DevisResponse.LigneResponse;
import com.faro.organisation.Organisation;

/**
 * Export du devis (lignes retenues) en CSV « Excel français », pour le reprendre dans un autre logiciel
 * (ProGBat…) ou un tableur. Le format exact d'import de ProGBat reste à confirmer avec un exemple.
 */
@Service
@Transactional(readOnly = true)
public class DevisCsvService {

    static final List<String> COLONNES = List.of("lot_code", "lot", "piece", "designation", "description", "unite",
        "quantite", "prix_unitaire_ht", "tva", "montant_ht", "gamme");

    private final DevisService devisService;

    public DevisCsvService(DevisService devisService) {
        this.devisService = devisService;
    }

    public byte[] csv(Organisation organisation, UUID affaireId) {
        DevisResponse devis = devisService.devis(organisation, affaireId);
        Map<UUID, DevisResponse.TotalLot> lots = devis.parLot().stream()
            .collect(Collectors.toMap(DevisResponse.TotalLot::lotId, l -> l));
        StringBuilder csv = new StringBuilder("﻿").append(ligne(COLONNES));
        for (LigneResponse l : devis.lignes()) {
            if (!l.incluse()) {
                continue;
            }
            DevisResponse.TotalLot lot = lots.get(l.lotId());
            String piece = devis.parPiece().stream().filter(p -> Objects.equals(p.pieceId(), l.pieceId()))
                .map(DevisResponse.TotalPiece::nom).findFirst().orElse("");
            csv.append(ligne(List.of(lot.code(), lot.nom(), piece, l.designation(),
                l.description() == null ? "" : l.description(), l.unite(), decimal(l.quantite()),
                decimal(l.prixUnitaireHt()), decimal(l.tauxTva()), decimal(l.montantHt()),
                l.gamme() == null ? "" : l.gamme().name())));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String ligne(List<String> champs) {
        return champs.stream().map(DevisCsvService::echapper).collect(Collectors.joining(";")) + "\r\n";
    }

    private static String echapper(String champ) {
        return champ.contains(";") || champ.contains("\"") || champ.contains("\n")
            ? "\"" + champ.replace("\"", "\"\"") + "\"" : champ;
    }

    private static String decimal(BigDecimal valeur) {
        return valeur.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
