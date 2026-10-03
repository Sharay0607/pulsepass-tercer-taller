package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {


    Optional<Artist> findByStageName(String stageName);

    // FR-SVC-009: búsqueda por nombre artístico sin distinguir mayúsculas
    Optional<Artist> findByStageNameIgnoreCase(String stageName);

    // BR-ARTIST-002: solo artistas activos, ordenados por nombre artístico
    List<Artist> findByActiveTrueOrderByStageNameAsc();
}
