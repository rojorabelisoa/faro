package com.faro.devis;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Nouvel ordre des lignes : la première reçoit l'ordre 1, etc. */
public record OrdreLignesRequest(@NotNull List<UUID> ids) {
}
