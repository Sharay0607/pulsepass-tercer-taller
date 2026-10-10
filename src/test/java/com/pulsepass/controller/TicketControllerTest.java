package com.pulsepass.controller;

import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
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

import static com.pulsepass.controller.ControllerTestData.ticket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    private static final String VALID_BODY = """
            {
              "userEmail": "andrea@email.com",
              "eventCode": "CMF-2026",
              "type": "VIP"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    // ================================================================ POST

    @Test // TEST-CTRL-TKT-001 · AC-CTRL-009 · UC-CTRL-07
    void shouldPurchaseTicket() throws Exception {
        // ARRANGE
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenReturn(ticket("TCK-1", TicketStatus.PAID));

        // ACT + ASSERT
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-1"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.type").value("VIP"))
                .andExpect(jsonPath("$.price").value(100000.00))
                .andExpect(jsonPath("$.userEmail").value("andrea@email.com"))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        // el JSON se convirtió bien al DTO y se delegó al Service
        ArgumentCaptor<PurchaseTicketRequest> captor = ArgumentCaptor.forClass(PurchaseTicketRequest.class);
        verify(ticketService).purchase(captor.capture());
        assertThat(captor.getValue().userEmail()).isEqualTo("andrea@email.com");
        assertThat(captor.getValue().eventCode()).isEqualTo("CMF-2026");
        assertThat(captor.getValue().type()).isEqualTo(TicketType.VIP);
    }

    @Test // TEST-CTRL-TKT-002
    void shouldReturn400WhenPurchaseRequestIsInvalid() throws Exception {
        // ACT + ASSERT: email inválido, eventCode vacío y type nulo
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userEmail": "not-an-email",
                                  "eventCode": "",
                                  "type": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.userEmail").value("User email must be valid"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(ticketService, never()).purchase(any());
    }

    @Test // body vacío: todos los campos obligatorios
    void shouldReturn400WhenPurchaseBodyIsEmpty() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.userEmail").value("User email is required"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(ticketService, never()).purchase(any());
    }

    @Test // el precio NO es un campo del request: un tipo inexistente es JSON inválido
    void shouldReturn400WhenTicketTypeIsNotAnEnumValue() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("VIP", "PLATINUM")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"));

        verify(ticketService, never()).purchase(any());
    }

    @Test // TEST-CTRL-TKT-003
    void shouldReturn404WhenUserDoesNotExist() throws Exception {
        // ARRANGE
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@email.com"));

        // ACT + ASSERT
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found: andrea@email.com"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test // TEST-CTRL-TKT-004 · AC-CTRL-010: Laura (17) no cumple la edad mínima → 409
    void shouldReturn409WhenUserDoesNotMeetMinimumAge() throws Exception {
        // ARRANGE
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("User does not meet minimum age."));

        // ACT + ASSERT
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("andrea@email.com", "laura@email.com")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User does not meet minimum age."))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test // BR-TICKET-007: sin cupos → 409
    void shouldReturn409WhenEventHasNoCapacityLeft() throws Exception {
        // ARRANGE
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("Event has no capacity left: CMF-2026"));

        // ACT + ASSERT
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Event has no capacity left: CMF-2026"));
    }

    // =========================================================== GET /{code}

    @Test // TEST-CTRL-TKT-005
    void shouldReturnTicketByCode() throws Exception {
        // ARRANGE
        when(ticketService.findByCode("TCK-1")).thenReturn(ticket("TCK-1", TicketStatus.PAID));

        // ACT + ASSERT
        mockMvc.perform(get("/api/tickets/{ticketCode}", "TCK-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-1"))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(ticketService).findByCode("TCK-1");
    }

    @Test // FR-CTRL-TKT-002: "retorna 200 o 404"
    void shouldReturn404WhenTicketDoesNotExist() throws Exception {
        // ARRANGE
        when(ticketService.findByCode("NOPE"))
                .thenThrow(new ResourceNotFoundException("Ticket not found: NOPE"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/tickets/{ticketCode}", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found: NOPE"));
    }

    // ======================================================= GET /by-user?...

    @Test // TEST-CTRL-TKT-006 · UC-CTRL-08
    void shouldReturnTicketsByUserEmail() throws Exception {
        // ARRANGE
        when(ticketService.findByUserEmail("andrea@email.com")).thenReturn(List.of(
                ticket("TCK-2", TicketStatus.PAID),
                ticket("TCK-1", TicketStatus.USED)));

        // ACT + ASSERT: "/by-user" no se confunde con "/{ticketCode}"
        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-2"))
                .andExpect(jsonPath("$[1].status").value("USED"));

        verify(ticketService).findByUserEmail("andrea@email.com");
        verify(ticketService, never()).findByCode(anyString());
    }

    @Test // un usuario sin tickets es una respuesta válida: 200 con lista vacía
    void shouldReturnEmptyListWhenUserHasNoTickets() throws Exception {
        // ARRANGE
        when(ticketService.findByUserEmail("carlos@email.com")).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/api/tickets/by-user").param("email", "carlos@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test // falta el query param obligatorio → 400, no 500
    void shouldReturn400WhenEmailParamIsMissing() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(get("/api/tickets/by-user"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing request parameter"))
                .andExpect(jsonPath("$.details.email").value("Parameter is required"));

        verify(ticketService, never()).findByUserEmail(any());
    }

    // ============================================================ PATCH cancel

    @Test // TEST-CTRL-TKT-008 · AC-CTRL-011 · UC-CTRL-09
    void shouldCancelPaidTicket() throws Exception {
        // ARRANGE
        when(ticketService.cancel("TCK-1")).thenReturn(ticket("TCK-1", TicketStatus.CANCELLED));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TCK-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-1"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TCK-1");
    }

    @Test // TEST-CTRL-TKT-009 · AC-CTRL-012: un ticket USED no se cancela
    void shouldReturn409WhenCancellingUsedTicket() throws Exception {
        // ARRANGE
        when(ticketService.cancel("TCK-1")).thenThrow(new BusinessRuleException(
                "Only PAID tickets can be cancelled. Ticket TCK-1 is USED."));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TCK-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Only PAID tickets can be cancelled. Ticket TCK-1 is USED."))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturn404WhenCancellingMissingTicket() throws Exception {
        // ARRANGE
        when(ticketService.cancel("NOPE"))
                .thenThrow(new ResourceNotFoundException("Ticket not found: NOPE"));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found: NOPE"));
    }

    // ============================================================== PATCH use

    @Test // TEST-CTRL-TKT-010 · AC-CTRL-013 · UC-CTRL-10
    void shouldMarkPaidTicketAsUsed() throws Exception {
        // ARRANGE
        when(ticketService.markAsUsed("TCK-1")).thenReturn(ticket("TCK-1", TicketStatus.USED));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TCK-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-1"))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TCK-1");
    }

    @Test // TEST-CTRL-TKT-011 · AC-CTRL-014: un ticket CANCELLED no se usa
    void shouldReturn409WhenUsingCancelledTicket() throws Exception {
        // ARRANGE
        when(ticketService.markAsUsed("TCK-1"))
                .thenThrow(new BusinessRuleException("A CANCELLED ticket cannot be used: TCK-1"));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TCK-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A CANCELLED ticket cannot be used: TCK-1"));
    }

    @Test
    void shouldReturn404WhenUsingMissingTicket() throws Exception {
        // ARRANGE
        when(ticketService.markAsUsed("NOPE"))
                .thenThrow(new ResourceNotFoundException("Ticket not found: NOPE"));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found: NOPE"));
    }
}
