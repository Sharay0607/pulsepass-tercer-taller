package com.pulsepass;

import com.pulsepass.domain.*;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VenueEventRelationTest extends AbstractIntegrationTest {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void shouldPersistVenueAndRetrieveByCode() {
        Venue venue = new Venue();
        venue.setCode("VEN-SMR-01");
        venue.setName("Marina Convention Center");
        venue.setCity("Santa Marta");
        venue.setCapacity(5000);
        venueRepository.save(venue);

        Optional<Venue> found = venueRepository.findByCode("VEN-SMR-01");

        assertThat(found).isPresent();
        assertThat(found.get().getCapacity()).isGreaterThan(0);
    }

    @Test
    void shouldAssociateEventToVenueAndRetrieveBoth() {
        Venue venue = new Venue();
        venue.setCode("VEN-CMF-01");
        venue.setName("City Music Festival Ground");
        venue.setCity("Santa Marta");
        venue.setCapacity(3000);
        venueRepository.save(venue);

        Event event = new Event();
        event.setEventCode("CMF-2026");
        event.setName("Caribbean Music Fest 2026");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusMonths(2));
        event.setVenue(venue);
        eventRepository.save(event);

        Optional<Event> found = eventRepository.findByEventCode("CMF-2026");

        assertThat(found).isPresent();
        assertThat(found.get().getVenue().getCode()).isEqualTo("VEN-CMF-01");

        List<Event> eventsOfVenue = eventRepository.findByVenue_Code("VEN-CMF-01");
        assertThat(eventsOfVenue).hasSize(1);
    }
}