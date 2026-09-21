package com.pulsepass;

import com.pulsepass.domain.*;
import com.pulsepass.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketRepositoryTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private TicketRepository ticketRepository;

    @Test
    void shouldRejectDuplicateTicketCode() {
        Ticket t1 = buildBaseTicket("TCK-0001");
        ticketRepository.saveAndFlush(t1);

        Ticket t2 = buildBaseTicket("TCK-0001"); // mismo código
        assertThatThrownBy(() -> ticketRepository.saveAndFlush(t2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldCountOnlyPaidTicketsForEvent() {
        Venue venue = venueRepository.save(newVenue());
        Event event = eventRepository.save(newEvent(venue));
        User user = userRepository.save(newUser());

        ticketRepository.save(newTicket("TCK-P1", event, user, TicketStatus.PAID));
        ticketRepository.save(newTicket("TCK-P2", event, user, TicketStatus.PAID));
        ticketRepository.save(newTicket("TCK-R1", event, user, TicketStatus.RESERVED));
        ticketRepository.save(newTicket("TCK-C1", event, user, TicketStatus.CANCELLED));

        long paidCount = ticketRepository.countPaidTicketsByEventCode(event.getEventCode());

        assertThat(paidCount).isEqualTo(2);
    }

    private Ticket buildBaseTicket(String code) {
        Venue venue = venueRepository.save(newVenue());
        Event event = eventRepository.save(newEvent(venue));
        User user = userRepository.save(newUser());
        return newTicket(code, event, user, TicketStatus.PAID);
    }

    private Venue newVenue() {
        Venue v = new Venue();
        v.setCode("VEN-" + System.nanoTime());
        v.setName("Test Venue");
        v.setCity("Santa Marta");
        v.setCapacity(1000);
        return v;
    }

    private Event newEvent(Venue venue) {
        Event e = new Event();
        e.setEventCode("EVT-" + System.nanoTime());
        e.setName("Test Event");
        e.setCategory(EventCategory.MUSIC);
        e.setStatus(EventStatus.PUBLISHED);
        e.setEventDate(LocalDateTime.now().plusDays(5));
        e.setVenue(venue);
        return e;
    }

    private User newUser() {
        User u = new User();
        u.setUsername("user" + System.nanoTime());
        u.setEmail("user" + System.nanoTime() + "@pulsepass.com");
        return u;
    }

    private Ticket newTicket(String code, Event event, User user, TicketStatus status) {
        Ticket t = new Ticket();
        t.setTicketCode(code);
        t.setType(TicketType.GENERAL);
        t.setPrice(new BigDecimal("120000.00"));
        t.setStatus(status);
        t.setPurchaseDate(LocalDateTime.now());
        t.setEvent(event);
        t.setUser(user);
        return t;
    }
}