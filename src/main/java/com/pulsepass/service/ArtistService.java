package com.pulsepass.service;

import com.pulsepass.dto.response.ArtistResponse;

import java.util.List;

public interface ArtistService {

    /** @throws com.pulsepass.exception.ResourceNotFoundException si no existe (BR-ARTIST-001) */
    ArtistResponse findById(Long id);

    /** @throws com.pulsepass.exception.ResourceNotFoundException si no existe (BR-ARTIST-001) */
    ArtistResponse findByStageName(String stageName);

    /** Solo artistas activos (BR-ARTIST-002). */
    List<ArtistResponse> findActiveArtists();
}
