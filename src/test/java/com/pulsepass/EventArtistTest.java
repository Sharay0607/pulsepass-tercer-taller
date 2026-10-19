package com.pulsepass;

import com.pulsepass.domain.*;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


public class EventArtistTest extends AbstractIntegrationTest {

    @Autowired private EventRepository eventRepository;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private VenueRepository venueRepository;

    @Test
    void shouldAssociateMultipleArtistsWithoutDuplicating() {
        Venue venue = new Venue();
        venue.setCode("VEN-" + System.nanoTime());
        venue.setName("Test Venue");
        venue.setCity("Santa Marta");
        venue.setCapacity(1000);
        venueRepository.save(venue);

        Artist a1 = new Artist();
        a1.setStageName("Test Artist A " + System.nanoTime());
        artistRepository.save(a1);

        Artist a2 = new Artist();
        a2.setStageName("Test Artist B " + System.nanoTime());
        artistRepository.save(a2);

        Event event = new Event();
        event.setEventCode("TEST-EVT-" + System.nanoTime());
        event.setName("Test Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setVenue(venue);
        event.setArtists(Set.of(a1, a2));

        eventRepository.save(event);

        List<Event> found = eventRepository.findEventsByArtistStageName(a1.getStageName());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getArtists()).hasSize(2);
    }
}