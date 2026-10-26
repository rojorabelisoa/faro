package com.faro.devis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MontantsTest {

    @Test
    void roundsLineAmountsToTheCent() {
        assertThat(Montants.montantHt(new BigDecimal("4.25"), new BigDecimal("72.40"))).isEqualByComparingTo("307.70");
        assertThat(Montants.montantHt(new BigDecimal("0.333"), new BigDecimal("10.00"))).isEqualByComparingTo("3.33");
        assertThat(Montants.montantHt(new BigDecimal("0.125"), new BigDecimal("1.00"))).isEqualByComparingTo("0.13");
    }

    @Test
    void computesVatOnTheBaseAmount() {
        assertThat(Montants.tva(new BigDecimal("980.00"), new BigDecimal("5.50"))).isEqualByComparingTo("53.90");
        assertThat(Montants.tva(new BigDecimal("1630.20"), new BigDecimal("10.00"))).isEqualByComparingTo("163.02");
    }

    @Test
    void acceptsOnlyRenovationVatRates() {
        assertThat(Montants.tauxValide(new BigDecimal("5.5"))).isTrue();
        assertThat(Montants.tauxValide(new BigDecimal("10"))).isTrue();
        assertThat(Montants.tauxValide(new BigDecimal("20.00"))).isTrue();
        assertThat(Montants.tauxValide(new BigDecimal("0"))).isTrue();
        assertThat(Montants.tauxValide(new BigDecimal("7"))).isFalse();
    }
}
