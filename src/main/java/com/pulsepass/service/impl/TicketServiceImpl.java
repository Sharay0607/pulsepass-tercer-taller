package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.User;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.TicketPriceCalculator;
import com.pulsepass.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketMapper ticketMapper;
    private final TicketPriceCalculator priceCalculator;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             UserRepository userRepository,
                             EventRepository eventRepository,
                             TicketMapper ticketMapper,
                             TicketPriceCalculator priceCalculator) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
        this.priceCalculator = priceCalculator;
    }

    // ---------------------------------------------------------------- purchase

    /**
     * Compra atómica (PRD §24 y §39): si cualquier validación falla se hace rollback
     * y nada queda persistido, incluido el cambio a SOLD_OUT.
     */
    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        // BR-TICKET-001 / 002: usuario existente y activo
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> ResourceNotFoundException.of("User", request.userEmail()));
        if (!user.isActive()) {
            throw new BusinessRuleException("User is not active: " + user.getEmail());
        }

        // BR-TICKET-003: evento existente
        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> ResourceNotFoundException.of("Event", request.eventCode()));

        // BR-TICKET-004: solo eventos PUBLISHED
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Tickets can only be purchased for PUBLISHED events. Event "
                            + event.getEventCode() + " is " + event.getStatus() + ".");
        }

        // BR-TICKET-005: el evento no puede haber ocurrido ya
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Event has already taken place: " + event.getEventCode());
        }

        // BR-TICKET-006: edad mínima evaluada en la fecha del evento
        validateMinimumAge(user, event);

        // BR-TICKET-007: capacidad
        int capacity = event.getVenue().getCapacity();
        long paidTickets = ticketRepository.countPaidTicketsByEventCode(event.getEventCode());
        if (paidTickets >= capacity) {
            throw new BusinessRuleException("Event has no capacity left: " + event.getEventCode());
        }

        // BR-TICKET-009: precio calculado por el sistema (nunca viene del cliente)
        BigDecimal price = priceCalculator.calculate(request.type());

        Ticket ticket = new Ticket();
        ticket.setTicketCode(generateTicketCode());
        ticket.setType(request.type());
        ticket.setPrice(price);
        ticket.setStatus(TicketStatus.PAID);          // PRD §27: compra válida → PAID
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setUser(user);
        ticket.setEvent(event);
        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: si esta compra llena el aforo → SOLD_OUT, misma transacción
        if (paidTickets + 1 >= capacity) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return ticketMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------- read

    @Override
    public TicketResponse findByCode(String ticketCode) {
        return ticketMapper.toResponse(getTicketOrThrow(ticketCode));
    }

    @Override
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findByEvent_EventCodeAndStatus(eventCode, TicketStatus.PAID)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------ cancel

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = getTicketOrThrow(ticketCode);

        // BR-TICKET-010 / 011: solo PAID → CANCELLED (USED y CANCELLED no se cancelan)
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled. Ticket " + ticketCode
                            + " is " + ticket.getStatus() + ".");
        }
        // BR-TICKET-012: no se cancela después de la fecha del evento
        if (ticket.getEvent().getEventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Ticket cannot be cancelled after the event date: " + ticketCode);
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    // -------------------------------------------------------------- markAsUsed

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = getTicketOrThrow(ticketCode);

        // BR-TICKET-014: un ticket CANCELLED nunca puede usarse
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessRuleException("A CANCELLED ticket cannot be used: " + ticketCode);
        }
        // BR-TICKET-013: solo PAID → USED
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be used. Ticket " + ticketCode
                            + " is " + ticket.getStatus() + ".");
        }

        ticket.setStatus(TicketStatus.USED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    // ----------------------------------------------------------------- helpers

    private Ticket getTicketOrThrow(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> ResourceNotFoundException.of("Ticket", ticketCode));
    }

    /** La edad se calcula con {@code UserProfile.birthDate} en la fecha del evento. */
    private void validateMinimumAge(User user, Event event) {
        Integer minimumAge = event.getMinimumAge();
        if (minimumAge == null || minimumAge <= 0) {
            return;
        }
        LocalDate birthDate = user.getProfile() == null ? null : user.getProfile().getBirthDate();
        if (birthDate == null) {
            throw new BusinessRuleException(
                    "User birth date is required to purchase tickets for age-restricted events.");
        }
        int ageAtEvent = Period.between(birthDate, event.getEventDate().toLocalDate()).getYears();
        if (ageAtEvent < minimumAge) {
            throw new BusinessRuleException("User does not meet minimum age.");
        }
    }

    private static String generateTicketCode() {
        return "TCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}

