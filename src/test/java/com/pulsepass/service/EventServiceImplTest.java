package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.impl.EventServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.pulsepass.service.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private ArtistRepository artistRepository;
    @Mock private EventMapper eventMapper;

    @InjectMocks private EventServiceImpl eventService;

    // ---------------------------------------------------------------- findByCode

    @Test // TEST-EVENT-001
    void findByCode_existingEvent_returnsDto() {
        // ARRANGE
        Event event = event("CMF-2026", EventStatus.DRAFT, FUTURE, 18, venue("VEN-SMR-01", 3, true));
        EventResponse expected = eventResponse("CMF-2026", EventStatus.DRAFT);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse result = eventService.findByCode("CMF-2026");

        // ASSERT
        assertThat(result).isSameAs(expected);
        verify(eventRepository).findByEventCode("CMF-2026");
    }

    @Test // TEST-EVENT-002
    void findByCode_missingEvent_throwsResourceNotFound() {
        // ARRANGE
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.findByCode("CMF-2026"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: CMF-2026");
        verify(eventMapper, never()).toResponse(any());
    }

    // -------------------------------------------------------------------- create

    @Test // TEST-EVENT-003 + BR-EVENT-005 + BR-EVENT-006
    void create_validRequest_savesEventAsDraft() {
        // ARRANGE
        Venue venue = venue("VEN-SMR-01", 3, true);
        EventResponse expected = eventResponse("CMF-2026", EventStatus.DRAFT);
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(expected);

        // ACT
        EventResponse result = eventService.create(validRequest(FUTURE, 18));

        // ASSERT
        assertThat(result).isSameAs(expected);
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        Event saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(saved.getEventCode()).isEqualTo("CMF-2026");
        assertThat(saved.getVenue()).isSameAs(venue);
        assertThat(saved.getMinimumAge()).isEqualTo(18);
    }

    @Test // BR-EVENT-006: null = sin restricción
    void create_nullMinimumAge_defaultsToZero() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue("VEN-SMR-01", 3, true)));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(eventResponse("CMF-2026", EventStatus.DRAFT));

        // ACT
        eventService.create(validRequest(FUTURE, null));

        // ASSERT
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getMinimumAge()).isZero();
    }

    @Test // BR-EVENT-001
    void create_duplicateCode_throwsDuplicateResource() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(validRequest(FUTURE, 18)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CMF-2026");
        verify(eventRepository, never()).save(any());
    }

    @Test // TEST-EVENT-004
    void create_missingVenue_throwsResourceNotFoundAndDoesNotSave() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(validRequest(FUTURE, 18)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("VEN-SMR-01");
        verify(eventRepository, never()).save(any());
    }

    @Test // TEST-EVENT-005
    void create_inactiveVenue_throwsBusinessRule() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue("VEN-SMR-01", 3, false)));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(validRequest(FUTURE, 18)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");
        verify(eventRepository, never()).save(any());
    }

    @Test // TEST-EVENT-006
    void create_pastDate_throwsBusinessRule() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue("VEN-SMR-01", 3, true)));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(validRequest(PAST, 18)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");
        verify(eventRepository, never()).save(any());
    }

    @Test // BR-EVENT-006
    void create_negativeMinimumAge_throwsBusinessRule() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue("VEN-SMR-01", 3, true)));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(validRequest(FUTURE, -1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Minimum age");
        verify(eventRepository, never()).save(any());
    }

    // ------------------------------------------------------------------- publish

    @Test // TEST-EVENT-007
    void publish_validDraft_becomesPublished() {
        // ARRANGE
        Event event = event("CMF-2026", EventStatus.DRAFT, FUTURE, 18, venue("VEN-SMR-01", 3, true));
        EventResponse expected = eventResponse("CMF-2026", EventStatus.PUBLISHED);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse result = eventService.publish("CMF-2026");

        // ASSERT
        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository).save(eq(event));
    }

    @Test // TEST-EVENT-008
    void publish_cancelledEvent_throwsBusinessRuleAndDoesNotPersist() {
        // ARRANGE
        Event event = event("CMF-2026", EventStatus.CANCELLED, FUTURE, 18, venue("VEN-SMR-01", 3, true));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DRAFT");
        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
        verify(eventRepository, never()).save(any());
    }

    @ParameterizedTest // BR-EVENT-007: ningún estado distinto de DRAFT se publica
    @EnumSource(value = EventStatus.class, names = {"PUBLISHED", "SOLD_OUT", "FINISHED"})
    void publish_nonDraftStatuses_throwBusinessRule(EventStatus status) {
        // ARRANGE
        Event event = event("CMF-2026", status, FUTURE, 18, venue("VEN-SMR-01", 3, true));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any());
    }

    @Test // BR-EVENT-008
    void publish_pastDate_throwsBusinessRule() {
        // ARRANGE
        Event event = event("CMF-2026", EventStatus.DRAFT, PAST, 18, venue("VEN-SMR-01", 3, true));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");
        verify(eventRepository, never()).save(any());
    }

    @Test // BR-EVENT-009
    void publish_inactiveVenue_throwsBusinessRule() {
        // ARRANGE
        Event event = event("CMF-2026", EventStatus.DRAFT, FUTURE, 18, venue("VEN-SMR-01", 3, false));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");
        verify(eventRepository, never()).save(any());
    }

    // ----------------------------------------------------------------- addArtist

    @Test // AC-003
    void addArtist_validArtist_associatesAndSaves() {
        // ARRANGE
        Event event = event("CMF-2026", EventStatus.DRAFT, FUTURE, 18, venue("VEN-SMR-01", 3, true));
        Artist solarBeat = artist(1L, "Solar Beat", true);
        EventResponse expected = eventResponse("CMF-2026", EventStatus.DRAFT);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(solarBeat));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse result = eventService.addArtist("CMF-2026", 1L);

        // ASSERT
        assertThat(result).isSameAs(expected);
        assertThat(event.getArtists()).containsExactly(solarBeat);
        verify(eventRepository).save(event);
    }

    @Test // BR-EVENT-010
    void addArtist_alreadyAssociated_throwsDuplicateResource() {
        // ARRANGE
        Event event = event("CMF-2026", EventStatus.DRAFT, FUTURE, 18, venue("VEN-SMR-01", 3, true));
        event.getArtists().add(artist(1L, "Solar Beat", true));
        // otra instancia con el mismo id: la comparación debe ser por id, no por identidad
        Artist sameArtist = artist(1L, "Solar Beat", true);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(sameArtist));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 1L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Solar Beat");
        assertThat(event.getArtists()).hasSize(1);
        verify(eventRepository, never()).save(any());
    }

    @ParameterizedTest // BR-EVENT-011
    @EnumSource(value = EventStatus.class, names = {"CANCELLED", "FINISHED"})
    void addArtist_closedEvent_throwsBusinessRule(EventStatus status) {
        // ARRANGE
        Event event = event("CMF-2026", status, FUTURE, 18, venue("VEN-SMR-01", 3, true));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist(1L, "Solar Beat", true)));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(status.name());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void addArtist_inactiveArtist_throwsBusinessRule() {
        // ARRANGE
        Event event = event("CMF-2026", EventStatus.DRAFT, FUTURE, 18, venue("VEN-SMR-01", 3, true));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(2L)).thenReturn(Optional.of(artist(2L, "Neon Waves", false)));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Neon Waves");
        verify(eventRepository, never()).save(any());
    }

    @Test
    void addArtist_missingEvent_throwsResourceNotFound() {
        // ARRANGE
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(artistRepository, never()).findById(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void addArtist_missingArtist_throwsResourceNotFound() {
        // ARRANGE
        Event event = event("CMF-2026", EventStatus.DRAFT, FUTURE, 18, venue("VEN-SMR-01", 3, true));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: 99");
        verify(eventRepository, never()).save(any());
    }

    // -------------------------------------------------------------------- listas

    @Test
    void findPublishedEvents_mapsEveryEventToSummary() {
        // ARRANGE
        Venue venue = venue("VEN-SMR-01", 3, true);
        Event e1 = event("CMF-2026", EventStatus.PUBLISHED, FUTURE, 18, venue);
        Event e2 = event("CMF-2027", EventStatus.PUBLISHED, FUTURE.plusDays(1), 0, venue);
        when(eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)).thenReturn(List.of(e1, e2));
        doReturn(eventSummary("CMF-2026")).when(eventMapper).toSummary(e1);
        doReturn(eventSummary("CMF-2027")).when(eventMapper).toSummary(e2);

        // ACT
        List<EventSummaryResponse> result = eventService.findPublishedEvents();

        // ASSERT
        assertThat(result).extracting(EventSummaryResponse::eventCode)
                .containsExactly("CMF-2026", "CMF-2027");
    }

    @Test
    void findByArtist_returnsSummariesOfThatArtist() {
        // ARRANGE
        Event e1 = event("CMF-2026", EventStatus.PUBLISHED, FUTURE, 18, venue("VEN-SMR-01", 3, true));
        when(eventRepository.findEventsByArtistStageName("Solar Beat")).thenReturn(List.of(e1));
        when(eventMapper.toSummary(e1)).thenReturn(eventSummary("CMF-2026"));

        // ACT
        List<EventSummaryResponse> result = eventService.findByArtist("Solar Beat");

        // ASSERT
        assertThat(result).hasSize(1);
        assertThat(result.get(0).eventCode()).isEqualTo("CMF-2026");
    }

    // ------------------------------------------------------------------- helpers

    private static CreateEventRequest validRequest(LocalDateTime date, Integer minimumAge) {
        return new CreateEventRequest("CMF-2026", "Caribbean Music Fest 2026", "Festival",
                EventCategory.MUSIC, date, minimumAge, "VEN-SMR-01");
    }
}
