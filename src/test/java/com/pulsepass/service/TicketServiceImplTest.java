package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class TicketServiceImplTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private TicketMapper ticketMapper;

    // El calculador es lógica pura: se usa el real, no un mock.
    private final TicketPriceCalculator priceCalculator = new TicketPriceCalculator(new BigDecimal("50000.00"));

    private TicketServiceImpl ticketService;

    private Venue venue;
    private Event event;                 // CMF-2026: PUBLISHED, +18, capacidad 3
    private User andrea;                 // 25 años al momento del evento

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(ticketRepository, userRepository, eventRepository,
                ticketMapper, priceCalculator);
        venue = venue("VEN-SMR-01", 3, true);
        event = event("CMF-2026", EventStatus.PUBLISHED, FUTURE, 18, venue);
        andrea = user("andrea@email.com", true, FUTURE.toLocalDate().minusYears(25));
    }

    // ------------------------------------------------------------------ purchase

    @Test // TEST-TICKET-001 + AC-004
    void purchase_validRequest_createsPaidTicketWithSystemPrice() {
        // ARRANGE
        TicketResponse expected = ticketResponse("TCK-1", TicketStatus.PAID);
        givenUserAndEvent(andrea, event);
        when(ticketRepository.countPaidTicketsByEventCode("CMF-2026")).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(expected);

        // ACT
        TicketResponse result = ticketService.purchase(purchase("andrea@email.com", TicketType.VIP));

        // ASSERT
        assertThat(result).isSameAs(expected);
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        Ticket saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(saved.getType()).isEqualTo(TicketType.VIP);
        assertThat(saved.getPrice()).isEqualByComparingTo("100000.00");   // base 50000 × 2.0
        assertThat(saved.getTicketCode()).startsWith("TCK-");
        assertThat(saved.getPurchaseDate()).isNotNull();
        assertThat(saved.getUser()).isSameAs(andrea);
        assertThat(saved.getEvent()).isSameAs(event);
        // quedan cupos → el evento NO se toca
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository, never()).save(any());
    }

    @Test // TEST-TICKET-002
    void purchase_missingUser_throwsResourceNotFound() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase("ghost@email.com")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(purchase("ghost@email.com", TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: ghost@email.com");
        verify(ticketRepository, never()).save(any());
    }

    @Test // TEST-TICKET-003 + AC-007
    void purchase_inactiveUser_throwsBusinessRule() {
        // ARRANGE
        User miguel = user("miguel@email.com", false, FUTURE.toLocalDate().minusYears(30));
        when(userRepository.findByEmailIgnoreCase("miguel@email.com")).thenReturn(Optional.of(miguel));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(purchase("miguel@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");
        verify(ticketRepository, never()).save(any());
        verify(eventRepository, never()).findByEventCode(any());
    }

    @Test // BR-TICKET-003
    void purchase_missingEvent_throwsResourceNotFound() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(andrea));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(purchase("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: CMF-2026");
        verify(ticketRepository, never()).save(any());
    }

    @Test // TEST-TICKET-004
    void purchase_draftEvent_throwsBusinessRule() {
        // ARRANGE
        event.setStatus(EventStatus.DRAFT);
        givenUserAndEvent(andrea, event);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(purchase("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PUBLISHED");
        verify(ticketRepository, never()).save(any());
    }

    @Test // TEST-TICKET-005
    void purchase_cancelledEvent_throwsBusinessRule() {
        // ARRANGE
        event.setStatus(EventStatus.CANCELLED);
        givenUserAndEvent(andrea, event);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(purchase("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");
        verify(ticketRepository, never()).save(any());
    }

    @ParameterizedTest // BR-TICKET-004: el resto de estados no vendibles
    @EnumSource(value = EventStatus.class, names = {"SOLD_OUT", "FINISHED"})
    void purchase_notPublishedEvent_throwsBusinessRule(EventStatus status) {
        // ARRANGE
        event.setStatus(status);
        givenUserAndEvent(andrea, event);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(purchase("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any());
    }

    @Test // BR-TICKET-005
    void purchase_eventAlreadyHappened_throwsBusinessRule() {
        // ARRANGE
        event.setEventDate(PAST);
        givenUserAndEvent(andrea, event);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(purchase("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already taken place");
        verify(ticketRepository, never()).save(any());
    }

    @Test // TEST-TICKET-006 + AC-006
    void purchase_underageUser_throwsBusinessRule() {
        // ARRANGE: Laura tiene 17 años el día del evento
        User laura = user("laura@email.com", true, FUTURE.toLocalDate().minusYears(17));
        givenUserAndEvent(laura, event);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(purchase("laura@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("User does not meet minimum age.");
        verify(ticketRepository, never()).save(any());
        verify(ticketRepository, never()).countPaidTicketsByEventCode(any());
    }

    @Test // BR-TICKET-006: la edad se evalúa en la fecha del evento, no hoy
    void purchase_userTurns18ExactlyOnEventDay_isAllowed() {
        // ARRANGE: hoy tiene 17, pero cumple 18 el mismo día del evento
        User turning18 = user("carlos@email.com", true, FUTURE.toLocalDate().minusYears(18));
        givenUserAndEvent(turning18, event);
        when(ticketRepository.countPaidTicketsByEventCode("CMF-2026")).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(ticketResponse("TCK-1", TicketStatus.PAID));

        // ACT
        ticketService.purchase(purchase("carlos@email.com", TicketType.GENERAL));

        // ASSERT
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test // BR-TICKET-006: un día antes de cumplir 18 en la fecha del evento → rechazado
    void purchase_userTurns18DayAfterEvent_isRejected() {
        // ARRANGE
        User almost18 = user("almost@email.com", true, FUTURE.toLocalDate().minusYears(18).plusDays(1));
        givenUserAndEvent(almost18, event);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(purchase("almost@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("User does not meet minimum age.");
        verify(ticketRepository, never()).save(any());
    }

    @Test // BR-TICKET-006: sin birthDate no se puede verificar la edad
    void purchase_ageRestrictedEventWithoutBirthDate_throwsBusinessRule() {
        // ARRANGE
        User noBirthDate = user("nodate@email.com", true, null);
        givenUserAndEvent(noBirthDate, event);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(purchase("nodate@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("birth date");
        verify(ticketRepository, never()).save(any());
    }

    @Test // BR-TICKET-006: minimumAge = 0 → no hay restricción, no se exige birthDate
    void purchase_eventWithoutAgeRestriction_doesNotRequireBirthDate() {
        // ARRANGE
        event.setMinimumAge(0);
        User noBirthDate = user("nodate@email.com", true, null);
        givenUserAndEvent(noBirthDate, event);
        when(ticketRepository.countPaidTicketsByEventCode("CMF-2026")).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(ticketResponse("TCK-1", TicketStatus.PAID));

        // ACT
        ticketService.purchase(purchase("nodate@email.com", TicketType.GENERAL));

        // ASSERT
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test // TEST-TICKET-007 + BR-TICKET-007
    void purchase_noCapacityLeft_throwsBusinessRule() {
        // ARRANGE: capacidad 3 y ya hay 3 PAID
        givenUserAndEvent(andrea, event);
        when(ticketRepository.countPaidTicketsByEventCode("CMF-2026")).thenReturn(3L);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(purchase("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no capacity");
        verify(ticketRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    @Test // TEST-TICKET-008 + BR-TICKET-008
    void purchase_lastAvailableTicket_savesTicketAndMarksEventSoldOut() {
        // ARRANGE: capacidad 3, 2 PAID → esta compra llena el aforo
        givenUserAndEvent(andrea, event);
        when(ticketRepository.countPaidTicketsByEventCode("CMF-2026")).thenReturn(2L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.save(event)).thenReturn(event);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(ticketResponse("TCK-3", TicketStatus.PAID));

        // ACT
        ticketService.purchase(purchase("andrea@email.com", TicketType.GENERAL));

        // ASSERT
        verify(ticketRepository).save(any(Ticket.class));
        verify(eventRepository).save(eq(event));
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
    }

    @Test // TEST-TICKET-008 (variante): una compra que NO llena el aforo no cambia el estado
    void purchase_notLastTicket_keepsEventPublished() {
        // ARRANGE: capacidad 3, 1 PAID
        givenUserAndEvent(andrea, event);
        when(ticketRepository.countPaidTicketsByEventCode("CMF-2026")).thenReturn(1L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(ticketResponse("TCK-2", TicketStatus.PAID));

        // ACT
        ticketService.purchase(purchase("andrea@email.com", TicketType.GENERAL));

        // ASSERT
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository, never()).save(any());
    }

    @Test // BR-TICKET-009 / PRD §22: type es obligatorio para calcular el precio
    void purchase_nullType_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE
        givenUserAndEvent(andrea, event);
        when(ticketRepository.countPaidTicketsByEventCode("CMF-2026")).thenReturn(0L);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(purchase("andrea@email.com", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("type");
        verify(ticketRepository, never()).save(any());
    }

    @Test // AC-004, AC-005, AC-008, AC-009: escenario completo con capacidad 3
    void purchase_acceptanceScenario_thirdSellsOutAndFourthIsRejected() {
        // ARRANGE: aforo 3; el repositorio "cuenta" 0, 1, 2 y luego 3 tickets pagados
        User carlos = user("carlos@email.com", true, FUTURE.toLocalDate().minusYears(21));
        User tercero = user("tercero@email.com", true, FUTURE.toLocalDate().minusYears(40));
        User cuarto = user("cuarto@email.com", true, FUTURE.toLocalDate().minusYears(35));
        doReturn(Optional.of(andrea)).when(userRepository).findByEmailIgnoreCase("andrea@email.com");
        doReturn(Optional.of(carlos)).when(userRepository).findByEmailIgnoreCase("carlos@email.com");
        doReturn(Optional.of(tercero)).when(userRepository).findByEmailIgnoreCase("tercero@email.com");
        doReturn(Optional.of(cuarto)).when(userRepository).findByEmailIgnoreCase("cuarto@email.com");
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.countPaidTicketsByEventCode("CMF-2026")).thenReturn(0L, 1L, 2L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.save(event)).thenReturn(event);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(ticketResponse("TCK-X", TicketStatus.PAID));

        // ACT
        ticketService.purchase(purchase("andrea@email.com", TicketType.VIP));
        ticketService.purchase(purchase("carlos@email.com", TicketType.GENERAL));
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        ticketService.purchase(purchase("tercero@email.com", TicketType.STUDENT));

        // ASSERT
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        assertThatThrownBy(() -> ticketService.purchase(purchase("cuarto@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, org.mockito.Mockito.times(3)).save(any(Ticket.class));
    }

    // -------------------------------------------------------------------- cancel

    @Test // TEST-TICKET-009
    void cancel_paidTicket_becomesCancelled() {
        // ARRANGE
        Ticket ticket = ticket("TCK-1", TicketStatus.PAID, event, andrea);
        TicketResponse expected = ticketResponse("TCK-1", TicketStatus.CANCELLED);
        when(ticketRepository.findByTicketCode("TCK-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        // ACT
        TicketResponse result = ticketService.cancel("TCK-1");

        // ASSERT
        assertThat(result.status()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository).save(eq(ticket));
    }

    @Test // TEST-TICKET-010 + AC-011
    void cancel_usedTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = ticket("TCK-1", TicketStatus.USED, event, andrea);
        when(ticketRepository.findByTicketCode("TCK-1")).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.cancel("TCK-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("USED");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository, never()).save(any());
    }

    @Test // BR-TICKET-011
    void cancel_alreadyCancelledTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = ticket("TCK-1", TicketStatus.CANCELLED, event, andrea);
        when(ticketRepository.findByTicketCode("TCK-1")).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.cancel("TCK-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");
        verify(ticketRepository, never()).save(any());
    }

    @Test // BR-TICKET-012
    void cancel_afterEventDate_throwsBusinessRule() {
        // ARRANGE
        event.setEventDate(PAST);
        Ticket ticket = ticket("TCK-1", TicketStatus.PAID, event, andrea);
        when(ticketRepository.findByTicketCode("TCK-1")).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.cancel("TCK-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("after the event date");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void cancel_missingTicket_throwsResourceNotFound() {
        when(ticketRepository.findByTicketCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.cancel("NOPE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ticket not found: NOPE");
        verify(ticketRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- markAsUsed

    @Test // TEST-TICKET-011 + AC-010
    void markAsUsed_paidTicket_becomesUsed() {
        // ARRANGE
        Ticket ticket = ticket("TCK-1", TicketStatus.PAID, event, andrea);
        TicketResponse expected = ticketResponse("TCK-1", TicketStatus.USED);
        when(ticketRepository.findByTicketCode("TCK-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        // ACT
        TicketResponse result = ticketService.markAsUsed("TCK-1");

        // ASSERT
        assertThat(result.status()).isEqualTo(TicketStatus.USED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository).save(ticket);
    }

    @Test // TEST-TICKET-012 + BR-TICKET-014
    void markAsUsed_cancelledTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = ticket("TCK-1", TicketStatus.CANCELLED, event, andrea);
        when(ticketRepository.findByTicketCode("TCK-1")).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.markAsUsed("TCK-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository, never()).save(any());
    }

    @Test // BR-TICKET-013
    void markAsUsed_alreadyUsedTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = ticket("TCK-1", TicketStatus.USED, event, andrea);
        when(ticketRepository.findByTicketCode("TCK-1")).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.markAsUsed("TCK-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only PAID");
        verify(ticketRepository, never()).save(any());
    }

    // --------------------------------------------------------------------- lecturas

    @Test
    void findByCode_existingTicket_returnsDto() {
        // ARRANGE
        Ticket ticket = ticket("TCK-1", TicketStatus.PAID, event, andrea);
        TicketResponse expected = ticketResponse("TCK-1", TicketStatus.PAID);
        when(ticketRepository.findByTicketCode("TCK-1")).thenReturn(Optional.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        // ACT + ASSERT
        assertThat(ticketService.findByCode("TCK-1")).isSameAs(expected);
    }

    @Test
    void findByUserEmail_mapsAllTicketsOfTheUser() {
        // ARRANGE
        Ticket t1 = ticket("TCK-1", TicketStatus.PAID, event, andrea);
        Ticket t2 = ticket("TCK-2", TicketStatus.USED, event, andrea);
        when(ticketRepository.findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc("andrea@email.com"))
                .thenReturn(List.of(t1, t2));
        doReturn(ticketResponse("TCK-1", TicketStatus.PAID)).when(ticketMapper).toResponse(t1);
        doReturn(ticketResponse("TCK-2", TicketStatus.USED)).when(ticketMapper).toResponse(t2);

        // ACT
        List<TicketResponse> result = ticketService.findByUserEmail("andrea@email.com");

        // ASSERT
        assertThat(result).extracting(TicketResponse::ticketCode).containsExactly("TCK-1", "TCK-2");
    }

    @Test
    void findPaidTicketsByEvent_queriesOnlyPaidStatus() {
        // ARRANGE
        Ticket t1 = ticket("TCK-1", TicketStatus.PAID, event, andrea);
        when(ticketRepository.findByEvent_EventCodeAndStatus("CMF-2026", TicketStatus.PAID))
                .thenReturn(List.of(t1));
        when(ticketMapper.toResponse(t1)).thenReturn(ticketResponse("TCK-1", TicketStatus.PAID));

        // ACT
        List<TicketResponse> result = ticketService.findPaidTicketsByEvent("CMF-2026");

        // ASSERT
        assertThat(result).hasSize(1);
        verify(ticketRepository).findByEvent_EventCodeAndStatus("CMF-2026", TicketStatus.PAID);
    }

    // -------------------------------------------------------------------- helpers

    private void givenUserAndEvent(User user, Event ev) {
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(ev.getEventCode())).thenReturn(Optional.of(ev));
    }

    private static PurchaseTicketRequest purchase(String email, TicketType type) {
        return new PurchaseTicketRequest(email, "CMF-2026", type);
    }
}
