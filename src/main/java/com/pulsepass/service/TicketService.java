package com.pulsepass.service;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface TicketService {

    @PreAuthorize("@authz.canPurchaseFor(#p0.userEmail())")
    TicketResponse purchase(PurchaseTicketRequest request);

    @PreAuthorize("@authz.canViewTicket(#p0)")
    TicketResponse findByCode(String ticketCode);

    @PreAuthorize("@authz.canAccessUserByEmail(#p0)")
    List<TicketResponse> findByUserEmail(String email);

    List<TicketResponse> findPaidTicketsByEvent(String eventCode);

    /** PAID → CANCELLED (BR-TICKET-010..012). */
    @PreAuthorize("@authz.canCancelTicket(#p0)")
    TicketResponse cancel(String ticketCode);

    /** PAID → USED (BR-TICKET-013..014). */
    TicketResponse markAsUsed(String ticketCode);
}
