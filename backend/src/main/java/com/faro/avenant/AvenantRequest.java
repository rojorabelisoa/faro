package com.faro.avenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AvenantRequest(@NotBlank @Size(max = 255) String objet, @Size(max = 20000) String motif) {
}
