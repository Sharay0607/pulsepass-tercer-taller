package com.pulsepass.controller;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.pulsepass.controller.ControllerTestData.artist;
import static com.pulsepass.controller.ControllerTestData.event;
import static com.pulsepass.controller.ControllerTestData.eventSummary;
import static com.pulsepass.controller.ControllerTestData.ticket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    private static final String VALID_BODY = """
            {
              "eventCode": "CMF-2026",
              "name": "Caribbean Music Fest 2026",
              "description": "Festival de musica",
              "category": "MUSIC",
              "eventDate": "2026-12-15T20:00:00",
              "minimumAge": 18,
              "venueCode": "VEN-SMR-01"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private TicketService ticketService;

    // ============================================================ POST /api/events

    @Test // TEST-CTRL-EVT-001 · AC-CTRL-003
    void shouldCreateEvent() throws Exception {
        // ARRANGE
        when(eventService.create(any(CreateEventRequest.class)))
                .thenReturn(event("CMF-2026", EventStatus.DRAFT, List.of()));

        // ACT + ASSERT
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.category").value("MUSIC"))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.minimumAge").value(18));

        ArgumentCaptor<CreateEventRequest> captor = ArgumentCaptor.forClass(CreateEventRequest.class);
        verify(eventService).create(captor.capture());
        assertThat(captor.getValue().eventCode()).isEqualTo("CMF-2026");
        assertThat(captor.getValue().category()).isEqualTo(EventCategory.MUSIC);
        assertThat(captor.getValue().minimumAge()).isEqualTo(18);
        assertThat(captor.getValue().venueCode()).isEqualTo("VEN-SMR-01");
    }

    @Test // TEST-CTRL-EVT-002 · AC-CTRL-004
    void shouldReturn400WhenEventRequestIsInvalid() throws Exception {
        // ACT + ASSERT: códigos vacíos y campos obligatorios nulos
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventCode": "",
                                  "name": "",
                                  "category": null,
                                  "minimumAge": null,
                                  "venueCode": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.name").value("Event name is required"))
                .andExpect(jsonPath("$.details.category").value("Category is required"))
                .andExpect(jsonPath("$.details.eventDate").value("Event date is required"))
                .andExpect(jsonPath("$.details.minimumAge").value("Minimum age is required"))
                .andExpect(jsonPath("$.details.venueCode").value("Venue code is required"));

        verify(eventService, never()).create(any());
    }

    @Test // §11.1: minimumAge @Min(0)
    void shouldReturn400WhenMinimumAgeIsNegative() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"minimumAge\": 18", "\"minimumAge\": -1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.minimumAge")
                        .value("Minimum age must be greater than or equal to 0"));

        verify(eventService, never()).create(any());
    }

    @Test // §11.1: description con longitud máxima
    void shouldReturn400WhenDescriptionIsTooLong() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("Festival de musica", "x".repeat(1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.description")
                        .value("Description cannot exceed 1000 characters"));

        verify(eventService, never()).create(any());
    }

    @Test // JSON con un valor de enum inexistente
    void shouldReturn400WhenCategoryIsNotAnEnumValue() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("MUSIC", "OPERA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.details.body").exists());

        verify(eventService, never()).create(any());
    }

    @Test // BR-EVENT-001 → DuplicateResourceException → 409
    void shouldReturn409WhenEventCodeAlreadyExists() throws Exception {
        // ARRANGE
        when(eventService.create(any(CreateEventRequest.class)))
                .thenThrow(new DuplicateResourceException("Event code already exists: CMF-2026"));

        // ACT + ASSERT
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Event code already exists: CMF-2026"));
    }

    @Test // BR-EVENT-002 → el venue no existe → 404
    void shouldReturn404WhenVenueDoesNotExist() throws Exception {
        // ARRANGE
        when(eventService.create(any(CreateEventRequest.class)))
                .thenThrow(new ResourceNotFoundException("Venue not found: VEN-SMR-01"));

        // ACT + ASSERT
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Venue not found: VEN-SMR-01"));
    }

    @Test // AC-CTRL-006: BusinessRuleException → 409 (venue inactivo, fecha pasada...)
    void shouldReturn409WhenCreateViolatesABusinessRule() throws Exception {
        // ARRANGE
        when(eventService.create(any(CreateEventRequest.class)))
                .thenThrow(new BusinessRuleException("Event date must be in the future."));

        // ACT + ASSERT
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Event date must be in the future."));
    }

    // ================================================ GET /api/events/{eventCode}

    @Test // TEST-CTRL-EVT-003
    void shouldReturnEventByCode() throws Exception {
        // ARRANGE
        when(eventService.findByCode("CMF-2026")).thenReturn(event("CMF-2026", EventStatus.DRAFT,
                List.of(artist(1L, "Solar Beat"))));

        // ACT + ASSERT
        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.name").value("Caribbean Music Fest 2026"))
                .andExpect(jsonPath("$.artists.length()").value(1))
                .andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

        verify(eventService).findByCode("CMF-2026");
    }

    @Test // TEST-CTRL-EVT-004
    void shouldReturn404WhenEventDoesNotExist() throws Exception {
        // ARRANGE
        when(eventService.findByCode("CMF-2026"))
                .thenThrow(new ResourceNotFoundException("Event not found: CMF-2026"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Event not found: CMF-2026"))
                .andExpect(jsonPath("$.details").isMap());
    }

    // ================================================== GET /api/events/published

    @Test // TEST-CTRL-EVT-005
    void shouldReturnPublishedEvents() throws Exception {
        // ARRANGE
        when(eventService.findPublishedEvents()).thenReturn(List.of(
                eventSummary("CMF-2026", EventStatus.PUBLISHED),
                eventSummary("CMF-2027", EventStatus.PUBLISHED)));

        // ACT + ASSERT: "/published" no se confunde con "/{eventCode}"
        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$[1].eventCode").value("CMF-2027"));

        verify(eventService).findPublishedEvents();
        verify(eventService, never()).findByCode(anyString());
    }

    // ============================================ PATCH /{eventCode}/publish

    @Test // TEST-CTRL-EVT-006 · AC-CTRL-005
    void shouldPublishEvent() throws Exception {
        // ARRANGE
        when(eventService.publish("CMF-2026"))
                .thenReturn(event("CMF-2026", EventStatus.PUBLISHED, List.of()));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    @Test // TEST-CTRL-EVT-007 · AC-CTRL-006
    void shouldReturn409WhenEventCannotBePublished() throws Exception {
        // ARRANGE
        when(eventService.publish("CMF-2026"))
                .thenThrow(new BusinessRuleException(
                        "Only DRAFT events can be published. Event CMF-2026 is CANCELLED."));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Only DRAFT events can be published. Event CMF-2026 is CANCELLED."))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturn404WhenPublishingMissingEvent() throws Exception {
        // ARRANGE
        when(eventService.publish("NOPE"))
                .thenThrow(new ResourceNotFoundException("Event not found: NOPE"));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/events/{eventCode}/publish", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found: NOPE"));
    }

    // =========================================== POST /{eventCode}/artists/{id}

    @Test // TEST-CTRL-EVT-008 · UC-CTRL-04: asociación válida → 200 (PRD §8.2, no 201)
    void shouldAddArtistToEvent() throws Exception {
        // ARRANGE
        when(eventService.addArtist("CMF-2026", 1L)).thenReturn(
                event("CMF-2026", EventStatus.DRAFT, List.of(artist(1L, "Solar Beat"))));

        // ACT + ASSERT
        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.artists.length()").value(1))
                .andExpect(jsonPath("$.artists[0].id").value(1))
                .andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    @Test // BR-EVENT-010: artista ya asociado → 409
    void shouldReturn409WhenArtistIsAlreadyAssociated() throws Exception {
        // ARRANGE
        when(eventService.addArtist("CMF-2026", 1L)).thenThrow(new DuplicateResourceException(
                "Artist Solar Beat is already associated with event CMF-2026"));

        // ACT + ASSERT
        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Artist Solar Beat is already associated with event CMF-2026"));
    }

    @Test // BR-EVENT-011: evento CANCELLED / FINISHED → 409
    void shouldReturn409WhenEventIsClosedForArtists() throws Exception {
        // ARRANGE
        when(eventService.addArtist("CMF-2026", 2L)).thenThrow(
                new BusinessRuleException("Cannot add artists to a CANCELLED event: CMF-2026"));

        // ACT + ASSERT
        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 2L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Cannot add artists to a CANCELLED event: CMF-2026"));
    }

    @Test
    void shouldReturn404WhenArtistToAddDoesNotExist() throws Exception {
        // ARRANGE
        when(eventService.addArtist("CMF-2026", 99L))
                .thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        // ACT + ASSERT
        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));
    }

    @Test // artistId no numérico → 400 y el Service nunca se invoca
    void shouldReturn400WhenArtistIdIsNotANumber() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.artistId").exists());

        verify(eventService, never()).addArtist(anyString(), anyLong());
    }

    // ===================================================== GET /by-artist?...

    @Test // TEST-CTRL-EVT-009
    void shouldReturnEventsByArtist() throws Exception {
        // ARRANGE
        when(eventService.findByArtist("Solar Beat"))
                .thenReturn(List.of(eventSummary("CMF-2026", EventStatus.PUBLISHED)));

        // ACT + ASSERT
        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findByArtist("Solar Beat");
    }

    @Test // falta el query param obligatorio → 400, no 500
    void shouldReturn400WhenStageNameParamIsMissing() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(get("/api/events/by-artist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing request parameter"))
                .andExpect(jsonPath("$.details.stageName").value("Parameter is required"));

        verify(eventService, never()).findByArtist(any());
    }

    // =========================================== GET /{eventCode}/tickets/paid

    @Test // TEST-CTRL-TKT-007 · FR-CTRL-TKT-004 (el endpoint vive bajo /api/events, PRD §8.5)
    void shouldReturnPaidTicketsOfEvent() throws Exception {
        // ARRANGE
        when(ticketService.findPaidTicketsByEvent("CMF-2026")).thenReturn(List.of(
                ticket("TCK-1", TicketStatus.PAID),
                ticket("TCK-2", TicketStatus.PAID)));

        // ACT + ASSERT
        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-1"))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[1].ticketCode").value("TCK-2"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
        verifyNoInteractions(eventService);
    }
}
