package com.faro.avenant;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.faro.common.Totaux;
import com.faro.devis.DevisResponse.LigneResponse;

public record AvenantResponse(
        UUID id,
        int numero,
        String objet,
        String motif,
        StatutAvenant statut,
        List<LigneResponse> lignes,
        Totaux totaux,
        Instant createdAt,
        Instant envoyeAt,
        Instant decideAt,
        String decidePar,
        String decisionCommentaire) {
}
