package com.pulsepass.controller;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.dto.response.VenueResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Escenario de referencia del PRD (§23): VEN-SMR-01, CMF-2026, Solar Beat / Neon Waves, Andrea...
 * Son los DTOs que "devolvería" el Service mockeado.
 */
final class ControllerTestData {

    static final LocalDateTime EVENT_DATE = LocalDateTime.of(2026, 12, 15, 20, 0);

    private ControllerTestData() {}

    static VenueResponse venue(String code, boolean active) {
        return new VenueResponse(1L, code, "Marina Convention Center", "Santa Marta",
                "Calle 1 # 2-3", 3, active);
    }

    static ArtistResponse artist(Long id, String stageName) {
        return new ArtistResponse(id, stageName, "Colombia", "Pop", true);
    }

    static EventResponse event(String eventCode, EventStatus status, List<ArtistResponse> artists) {
        return new EventResponse(10L, eventCode, "Caribbean Music Fest 2026", "Festival de musica",
                EventCategory.MUSIC, status, EVENT_DATE, 18,
                "VEN-SMR-01", "Marina Convention Center", artists);
    }

    static EventSummaryResponse eventSummary(String eventCode, EventStatus status) {
        return new EventSummaryResponse(10L, eventCode, "Caribbean Music Fest 2026",
                EventCategory.MUSIC, status, EVENT_DATE, "VEN-SMR-01", "Marina Convention Center");
    }

    static UserResponse user(String username, String email) {
        return new UserResponse(100L, username, email, true, "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.of(2000, 5, 10));
    }

    static TicketResponse ticket(String ticketCode, TicketStatus status) {
        return new TicketResponse(1000L, ticketCode, TicketType.VIP, new BigDecimal("100000.00"), status,
                LocalDateTime.of(2026, 10, 9, 12, 0), "andrea@email.com", "CMF-2026",
                "Caribbean Music Fest 2026");
    }
}
