package com.pulsepass;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EventArtistIT extends AbstractIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Test
    void shouldAssociateMultipleArtistsWithoutDuplicating() {
        Artist a1 = new Artist();
        a1.setStageName("Solar Beat");
        artistRepository.save(a1);

        Artist a2 = new Artist();
        a2.setStageName("Neon Waves");
        artistRepository.save(a2);

        Event event = new Event();
        event.setEventCode("TEST-EVT-01");
        event.setName("Test Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setArtists(Set.of(a1, a2));

        eventRepository.save(event);

        List<Event> found = eventRepository.findEventsByArtistStageName("Solar Beat");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getArtists()).hasSize(2);
    }
}