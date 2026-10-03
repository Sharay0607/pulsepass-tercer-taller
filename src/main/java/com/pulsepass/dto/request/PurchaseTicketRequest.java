package com.pulsepass.dto.request;

import com.pulsepass.domain.TicketType;

/**
 * El precio NO viaja en el request: lo calcula el sistema (PRD §22).
 */
public record PurchaseTicketRequest(
        String userEmail,
        String eventCode,
        TicketType type
) {}
