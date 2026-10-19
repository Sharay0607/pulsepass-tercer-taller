package com.pulsepass.dto.request;

import com.pulsepass.domain.TicketType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * El precio NO viaja en el request: lo calcula el sistema (PRD de Servicios §22).
 */
public record PurchaseTicketRequest(

        @NotBlank(message = "User email is required")
        @Email(message = "User email must be valid")
        @Size(max = 150, message = "User email cannot exceed 150 characters")
        String userEmail,

        @NotBlank(message = "Event code is required")
        @Size(max = 50, message = "Event code cannot exceed 50 characters")
        String eventCode,

        @NotNull(message = "Ticket type is required")
        TicketType type
) {}
