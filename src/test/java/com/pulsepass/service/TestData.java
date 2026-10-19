package com.pulsepass.service;

import com.pulsepass.domain.*;
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

/** Fábrica de objetos de prueba compartida por los unit tests de Service. */
final class TestData {

    static final LocalDateTime FUTURE = LocalDateTime.now().plusMonths(3).withNano(0);
    static final LocalDateTime PAST = LocalDateTime.now().minusDays(10).withNano(0);

    private TestData() {}

    static Venue venue(String code, int capacity, boolean active) {
        Venue v = new Venue();
        v.setId(1L);
        v.setCode(code);
        v.setName("Marina Convention Center");
        v.setCity("Santa Marta");
        v.setCapacity(capacity);
        v.setActive(active);
        return v;
    }

    static Event event(String code, EventStatus status, LocalDateTime date, Integer minimumAge, Venue venue) {
        Event e = new Event();
        e.setId(10L);
        e.setEventCode(code);
        e.setName("Caribbean Music Fest 2026");
        e.setCategory(EventCategory.MUSIC);
        e.setStatus(status);
        e.setEventDate(date);
        e.setMinimumAge(minimumAge);
        e.setVenue(venue);
        return e;
    }

    static Artist artist(Long id, String stageName, boolean active) {
        Artist a = new Artist();
        a.setId(id);
        a.setStageName(stageName);
        a.setActive(active);
        return a;
    }

    /** Usuario con perfil; {@code birthDate} puede ser null. */
    static User user(String email, boolean active, LocalDate birthDate) {
        User u = new User();
        u.setId(100L);
        u.setUsername(email.substring(0, email.indexOf('@')));
        u.setEmail(email);
        u.setActive(active);
        UserProfile p = new UserProfile();
        p.setBirthDate(birthDate);
        p.setUser(u);
        u.setProfile(p);
        return u;
    }

    static Ticket ticket(String code, TicketStatus status, Event event, User user) {
        Ticket t = new Ticket();
        t.setId(1000L);
        t.setTicketCode(code);
        t.setType(TicketType.GENERAL);
        t.setPrice(new BigDecimal("50000.00"));
        t.setStatus(status);
        t.setPurchaseDate(LocalDateTime.now());
        t.setEvent(event);
        t.setUser(user);
        return t;
    }

    // ---- DTOs de ejemplo (lo que devolvería el mapper mockeado) ----

    static VenueResponse venueResponse(String code) {
        return new VenueResponse(1L, code, "Marina Convention Center", "Santa Marta", null, 3, true);
    }

    static ArtistResponse artistResponse(Long id, String stageName) {
        return new ArtistResponse(id, stageName, null, null, true);
    }

    static EventResponse eventResponse(String code, EventStatus status) {
        return new EventResponse(10L, code, "Caribbean Music Fest 2026", null, EventCategory.MUSIC,
                status, FUTURE, 18, "VEN-SMR-01", "Marina Convention Center", List.of());
    }

    static EventSummaryResponse eventSummary(String code) {
        return new EventSummaryResponse(10L, code, "Caribbean Music Fest 2026", EventCategory.MUSIC,
                EventStatus.PUBLISHED, FUTURE, "VEN-SMR-01", "Marina Convention Center");
    }

    static UserResponse userResponse(String email) {
        return new UserResponse(100L, "user", email, true, null, null, null, null, null);
    }

    static TicketResponse ticketResponse(String code, TicketStatus status) {
        return new TicketResponse(1000L, code, TicketType.GENERAL, new BigDecimal("50000.00"), status,
                LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026");
    }
}
