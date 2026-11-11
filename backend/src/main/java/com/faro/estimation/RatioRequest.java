package com.faro.estimation;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record RatioRequest(
        @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal prixMinM2,
        @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal prixMaxM2) {
}
