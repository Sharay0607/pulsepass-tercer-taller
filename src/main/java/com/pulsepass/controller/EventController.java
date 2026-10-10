package com.pulsepass.controller;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final TicketService ticketService;

    public EventController(EventService eventService, TicketService ticketService) {
        this.eventService = eventService;
        this.ticketService = ticketService;
    }

    // EventService.create()  →  201 Created: la operación crea un recurso nuevo
    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // EventService.findByCode()
    @GetMapping("/{eventCode}")
    public ResponseEntity<EventResponse> findByCode(@PathVariable("eventCode") String eventCode) {
        return ResponseEntity.ok(eventService.findByCode(eventCode));
    }

    // EventService.findPublishedEvents()  (ruta literal: se prefiere sobre "/{eventCode}")
    @GetMapping("/published")
    public ResponseEntity<List<EventSummaryResponse>> findPublishedEvents() {
        return ResponseEntity.ok(eventService.findPublishedEvents());
    }

    // EventService.publish()  DRAFT → PUBLISHED
    @PatchMapping("/{eventCode}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable("eventCode") String eventCode) {
        return ResponseEntity.ok(eventService.publish(eventCode));
    }

    // EventService.addArtist()  →  200 OK (PRD §8.2): asocia recursos existentes, no crea uno nuevo
    @PostMapping("/{eventCode}/artists/{artistId}")
    public ResponseEntity<EventResponse> addArtist(
            @PathVariable("eventCode") String eventCode,
            @PathVariable("artistId") Long artistId) {
        return ResponseEntity.ok(eventService.addArtist(eventCode, artistId));
    }

    // EventService.findByArtist()  →  GET /api/events/by-artist?stageName=Solar Beat
    @GetMapping("/by-artist")
    public ResponseEntity<List<EventSummaryResponse>> findByArtist(
            @RequestParam("stageName") String stageName) {
        return ResponseEntity.ok(eventService.findByArtist(stageName));
    }

    // TicketService.findPaidTicketsByEvent()  (PRD §8.5: vive bajo /api/events/{eventCode})
    @GetMapping("/{eventCode}/tickets/paid")
    public ResponseEntity<List<TicketResponse>> findPaidTickets(
            @PathVariable("eventCode") String eventCode) {
        return ResponseEntity.ok(ticketService.findPaidTicketsByEvent(eventCode));
    }
}
