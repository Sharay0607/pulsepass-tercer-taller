package com.pulsepass.service;

import com.pulsepass.dto.response.VenueResponse;

import java.util.List;

public interface VenueService {

    /** @throws com.pulsepass.exception.ResourceNotFoundException si el venue no existe (BR-VENUE-001) */
    VenueResponse findByCode(String code);

    /** Solo venues con {@code active = true} (BR-VENUE-002). */
    List<VenueResponse> findActiveVenues();
}
