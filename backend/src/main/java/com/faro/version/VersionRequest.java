package com.faro.version;

import jakarta.validation.constraints.Size;

/** Libellé facultatif (« Devis envoyé au client », « Dossier travaux »…). */
public record VersionRequest(@Size(max = 255) String libelle) {
}
