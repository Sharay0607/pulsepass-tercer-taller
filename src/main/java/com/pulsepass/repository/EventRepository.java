package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // FR-EVT-002
    Optional<Event> findByEventCode(String eventCode);

    // FR-EVT-005: eventos publicados ordenados por fecha (Query Method puro)
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    // FR-VEN-004: eventos de un venue navegando la relación por código
    List<Event> findByVenue_Code(String venueCode);

    // FR-ART-004 / FR-SRC-001: eventos por artista (necesita JOIN -> JPQL)
    @Query("""
           SELECT DISTINCT e FROM Event e
           JOIN e.artists a
           WHERE a.stageName = :stageName
           """)
    List<Event> findEventsByArtistStageName(@Param("stageName") String stageName);

    // FR-SRC-002: eventos por ciudad Y artista (dos asociaciones -> JPQL)
    @Query("""
           SELECT DISTINCT e FROM Event e
           JOIN e.artists a
           WHERE e.venue.city = :city
           AND a.stageName = :stageName
           """)
    List<Event> findByVenueCityAndArtistStageName(@Param("city") String city,
                                                    @Param("stageName") String stageName);

    // FR-SRC-003: eventos recomendados (filtros + DISTINCT + orden -> JPQL)
    @Query("""
           SELECT DISTINCT e FROM Event e
           JOIN e.artists a
           WHERE e.status = 'PUBLISHED'
           AND e.eventDate > :fromDate
           AND e.venue.city = :city
           AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistText, '%'))
           ORDER BY e.eventDate ASC
           """)
    List<Event> findRecommendedEvents(@Param("fromDate") LocalDateTime fromDate,
                                        @Param("city") String city,
                                        @Param("artistText") String artistText);
}