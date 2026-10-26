package com.faro.lot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ordre facultatif : à la création, le lot est placé en dernier. */
public record LotRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 255) String nom,
        Integer ordre) {
}
