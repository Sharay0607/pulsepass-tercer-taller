package com.pulsepass.repository;

import com.pulsepass.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    // FR-VEN-001 / FR-VEN-004: buscar venue por código de negocio
    Optional<Venue> findByCode(String code);
}