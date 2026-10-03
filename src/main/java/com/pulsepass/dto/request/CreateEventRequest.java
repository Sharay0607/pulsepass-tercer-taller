package com.pulsepass.dto.request;

import com.pulsepass.domain.EventCategory;

import java.time.LocalDateTime;

/**
 * Datos para crear un evento. El estado inicial NO viene del request (BR-EVENT-005).
 */
public record CreateEventRequest(
        String eventCode,
        String name,
        String description,
        EventCategory category,
        LocalDateTime eventDate,
        Integer minimumAge,
        String venueCode
) {}
