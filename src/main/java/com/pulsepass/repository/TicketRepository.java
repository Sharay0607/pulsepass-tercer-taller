package com.pulsepass.repository;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // FR-TKT-002
    Optional<Ticket> findByTicketCode(String ticketCode);

    // FR-TKT-006: tickets de un usuario por email y status (navega dos relaciones)
    List<Ticket> findByUser_EmailAndStatus(String email, TicketStatus status);

    // FR-TKT-006 (variante sin status)
    List<Ticket> findByUser_Email(String email);

    // FR-TKT-007: tickets PAID de un evento por eventCode
    List<Ticket> findByEvent_EventCodeAndStatus(String eventCode, TicketStatus status);

    // FR-TKT-008: conteo de ventas -> JPQL con COUNT
    @Query("""
           SELECT COUNT(t) FROM Ticket t
           WHERE t.event.eventCode = :eventCode
           AND t.status = 'PAID'
           """)
    long countPaidTicketsByEventCode(@Param("eventCode") String eventCode);

    // FR-SRC-004 (Could): tickets de eventos futuros, ordenados
    @Query("""
           SELECT t FROM Ticket t
           WHERE t.event.eventDate > :fromDate
           ORDER BY t.event.eventDate ASC
           """)
    List<Ticket> findTicketsOfFutureEvents(@Param("fromDate") LocalDateTime fromDate);
}