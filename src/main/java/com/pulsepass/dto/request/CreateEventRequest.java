package com.pulsepass.dto.request;

import com.pulsepass.domain.EventCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Datos para crear un evento. El estado inicial NO viene del request (BR-EVENT-005).
 *
 * <p>Aquí solo hay validación <b>estructural</b> (obligatorio, longitud, mínimo).
 * Las reglas de negocio (fecha futura, venue activo, código único...) viven en el Service.
 * Las longitudes máximas coinciden con las columnas de {@code V1__create_schema.sql}.
 */
public record CreateEventRequest(

        @NotBlank(message = "Event code is required")
        @Size(max = 50, message = "Event code cannot exceed 50 characters")
        String eventCode,

        @NotBlank(message = "Event name is required")
        @Size(max = 150, message = "Event name cannot exceed 150 characters")
        String name,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @NotNull(message = "Category is required")
        EventCategory category,

        @NotNull(message = "Event date is required")
        LocalDateTime eventDate,

        @NotNull(message = "Minimum age is required")
        @Min(value = 0, message = "Minimum age must be greater than or equal to 0")
        Integer minimumAge,

        @NotBlank(message = "Venue code is required")
        @Size(max = 50, message = "Venue code cannot exceed 50 characters")
        String venueCode
) {}
