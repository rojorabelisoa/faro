package com.faro.avenant;

import java.util.Map;
import java.util.UUID;

/** Ce qui est figé à l'envoi : l'avenant et les noms des pièces et lots tels qu'ils étaient. */
public record AvenantDocument(String entreprise, String affaire, String client, String adresse, AvenantResponse avenant,
        Map<UUID, String> pieces, Map<UUID, String> lots) {
}
