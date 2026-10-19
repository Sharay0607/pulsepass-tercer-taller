package com.pulsepass.service;

import com.pulsepass.domain.TicketType;
import com.pulsepass.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * Estrategia de precios encapsulada (PRD §28). El cliente nunca envía el precio:
 * se calcula como {@code precio base × multiplicador del tipo de ticket}.
 *
 * <pre>
 * GENERAL   → 1.00 × base
 * STUDENT   → 0.70 × base   (30 % de descuento)
 * VIP       → 2.00 × base
 * BACKSTAGE → 3.50 × base
 * </pre>
 *
 * El precio base es configurable con {@code pulsepass.pricing.base-price}.
 */
@Component
public class TicketPriceCalculator {

    static final BigDecimal DEFAULT_BASE_PRICE = new BigDecimal("50000.00");

    private static final Map<TicketType, BigDecimal> MULTIPLIERS = new EnumMap<>(TicketType.class);

    static {
        MULTIPLIERS.put(TicketType.GENERAL, new BigDecimal("1.00"));
        MULTIPLIERS.put(TicketType.STUDENT, new BigDecimal("0.70"));
        MULTIPLIERS.put(TicketType.VIP, new BigDecimal("2.00"));
        MULTIPLIERS.put(TicketType.BACKSTAGE, new BigDecimal("3.50"));
    }

    private final BigDecimal basePrice;

    public TicketPriceCalculator(
            @Value("${pulsepass.pricing.base-price:50000.00}") BigDecimal basePrice) {
        if (basePrice == null || basePrice.signum() < 0) {
            throw new IllegalArgumentException("Base price must be >= 0.");
        }
        this.basePrice = basePrice;
    }

    public BigDecimal calculate(TicketType type) {
        if (type == null) {
            throw new BusinessRuleException("Ticket type is required.");
        }
        BigDecimal multiplier = MULTIPLIERS.get(type);
        if (multiplier == null) {
            throw new BusinessRuleException("No pricing rule for ticket type: " + type);
        }
        BigDecimal price = basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        // BR-TICKET-009: nunca precio negativo
        if (price.signum() < 0) {
            throw new BusinessRuleException("Ticket price cannot be negative.");
        }
        return price;
    }
}
