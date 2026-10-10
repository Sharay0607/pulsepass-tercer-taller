package com.pulsepass.service;

import com.pulsepass.domain.TicketType;
import com.pulsepass.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketPriceCalculatorTest {

    private final TicketPriceCalculator calculator = new TicketPriceCalculator(new BigDecimal("50000.00"));

    @ParameterizedTest
    @CsvSource({
            "GENERAL,   50000.00",
            "STUDENT,   35000.00",
            "VIP,      100000.00",
            "BACKSTAGE,175000.00"
    })
    void calculate_appliesMultiplierToBasePrice(TicketType type, String expected) {
        assertThat(calculator.calculate(type)).isEqualByComparingTo(expected);
    }

    @Test
    void calculate_alwaysReturnsScaleTwo() {
        assertThat(calculator.calculate(TicketType.STUDENT).scale()).isEqualTo(2);
    }

    @Test
    void calculate_roundsHalfUpToTwoDecimals() {
        TicketPriceCalculator cheap = new TicketPriceCalculator(new BigDecimal("0.05"));

        // 0.05 × 0.70 = 0.035 → 0.04
        assertThat(cheap.calculate(TicketType.STUDENT)).isEqualByComparingTo("0.04");
    }

    @Test
    void calculate_zeroBasePrice_givesFreeTicket() {
        TicketPriceCalculator free = new TicketPriceCalculator(BigDecimal.ZERO);

        assertThat(free.calculate(TicketType.BACKSTAGE)).isEqualByComparingTo("0.00");
    }

    @Test
    void calculate_nullType_throwsBusinessRule() {
        assertThatThrownBy(() -> calculator.calculate(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("type");
    }

    @Test // BR-TICKET-009: nunca precio negativo → ni siquiera se puede configurar
    void constructor_negativeBasePrice_isRejected() {
        assertThatThrownBy(() -> new TicketPriceCalculator(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
