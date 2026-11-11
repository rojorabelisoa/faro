package com.faro.devis;

import jakarta.validation.constraints.NotNull;

public record GammeRequest(@NotNull Gamme gamme) {
}
