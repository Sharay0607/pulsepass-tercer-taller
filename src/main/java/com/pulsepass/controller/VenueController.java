package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.service.VenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone {@link VenueService} por HTTP. Solo traduce HTTP ↔ DTO: sin reglas de negocio
 * y sin conocer repositories (CTRL-001, CTRL-002).
 */
@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    // VenueService.findByCode()
    @GetMapping("/{code}")
    public ResponseEntity<VenueResponse> findByCode(@PathVariable("code") String code) {
        return ResponseEntity.ok(venueService.findByCode(code));
    }

    // VenueService.findActiveVenues()  (ruta literal: Spring la prefiere sobre "/{code}")
    @GetMapping("/active")
    public ResponseEntity<List<VenueResponse>> findActiveVenues() {
        return ResponseEntity.ok(venueService.findActiveVenues());
    }
}
