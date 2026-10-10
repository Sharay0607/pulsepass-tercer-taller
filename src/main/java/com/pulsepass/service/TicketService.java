package com.pulsepass.service;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;

import java.util.List;

public interface TicketService {

    TicketResponse purchase(PurchaseTicketRequest request);

    TicketResponse findByCode(String ticketCode);

    List<TicketResponse> findByUserEmail(String email);

    List<TicketResponse> findPaidTicketsByEvent(String eventCode);

    /** PAID → CANCELLED (BR-TICKET-010..012). */
    TicketResponse cancel(String ticketCode);

    /** PAID → USED (BR-TICKET-013..014). */
    TicketResponse markAsUsed(String ticketCode);
}
