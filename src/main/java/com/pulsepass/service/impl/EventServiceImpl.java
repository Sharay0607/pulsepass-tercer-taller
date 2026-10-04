package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
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
import com.pulsepass.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(EventRepository eventRepository,
                            VenueRepository venueRepository,
                            ArtistRepository artistRepository,
                            EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
    }

    // ------------------------------------------------------------------ create

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        // BR-EVENT-001: código único
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException("Event code already exists: " + request.eventCode());
        }

        // BR-EVENT-002: venue obligatorio y existente
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> ResourceNotFoundException.of("Venue", request.venueCode()));

        // BR-EVENT-003: venue activo
        if (!venue.isActive()) {
            throw new BusinessRuleException("Venue is not active: " + venue.getCode());
        }

        // BR-EVENT-004: fecha futura
        requireFutureDate(request.eventDate());

        // BR-EVENT-006: edad mínima >= 0 (null se interpreta como "sin restricción")
        int minimumAge = request.minimumAge() == null ? 0 : request.minimumAge();
        if (minimumAge < 0) {
            throw new BusinessRuleException("Minimum age must be >= 0.");
        }

        Event event = new Event();
        event.setEventCode(request.eventCode());
        event.setName(request.name());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setEventDate(request.eventDate());
        event.setMinimumAge(minimumAge);
        event.setVenue(venue);
        // BR-EVENT-005: el estado inicial lo decide el sistema, no el request
        event.setStatus(EventStatus.DRAFT);

        return eventMapper.toResponse(eventRepository.save(event));
    }

    // -------------------------------------------------------------------- read

    @Override
    public EventResponse findByCode(String eventCode) {
        return eventMapper.toResponse(getEventOrThrow(eventCode));
    }

    @Override
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Override
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository.findEventsByArtistStageName(stageName)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    // ----------------------------------------------------------------- publish

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = getEventOrThrow(eventCode);

        // BR-EVENT-007: solo DRAFT → PUBLISHED
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published. Event " + eventCode
                            + " is " + event.getStatus() + ".");
        }
        // BR-EVENT-008: debe seguir siendo futuro
        requireFutureDate(event.getEventDate());
        // BR-EVENT-009: el venue debe continuar activo
        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException("Venue is not active: " + event.getVenue().getCode());
        }

        event.setStatus(EventStatus.PUBLISHED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    // --------------------------------------------------------------- addArtist

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = getEventOrThrow(eventCode);
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> ResourceNotFoundException.of("Artist", artistId));

        // BR-EVENT-011: no se agregan artistas a eventos cerrados
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException(
                    "Cannot add artists to a " + event.getStatus() + " event: " + eventCode);
        }
        if (!artist.isActive()) {
            throw new BusinessRuleException("Artist is not active: " + artist.getStageName());
        }
        // BR-EVENT-010: sin duplicados (se compara por id: Artist no define equals/hashCode)
        boolean alreadyAssociated = event.getArtists().stream()
                .anyMatch(a -> Objects.equals(a.getId(), artist.getId()));
        if (alreadyAssociated) {
            throw new DuplicateResourceException(
                    "Artist " + artist.getStageName() + " is already associated with event " + eventCode);
        }

        event.getArtists().add(artist);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    // ----------------------------------------------------------------- helpers

    private Event getEventOrThrow(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> ResourceNotFoundException.of("Event", eventCode));
    }

    private void requireFutureDate(LocalDateTime eventDate) {
        if (eventDate == null || !eventDate.isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Event date must be in the future.");
        }
    }
}
