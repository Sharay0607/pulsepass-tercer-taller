package com.pulsepass.service;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.VenueMapper;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.impl.VenueServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.pulsepass.service.TestData.venue;
import static com.pulsepass.service.TestData.venueResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    @Mock private VenueRepository venueRepository;
    @Mock private VenueMapper venueMapper;

    @InjectMocks private VenueServiceImpl venueService;

    @Test // FR-SVC-001
    void findByCode_existingVenue_returnsDto() {
        Venue venue = venue("VEN-SMR-01", 3, true);
        VenueResponse expected = venueResponse("VEN-SMR-01");
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(expected);

        assertThat(venueService.findByCode("VEN-SMR-01")).isSameAs(expected);
    }

    @Test // BR-VENUE-001
    void findByCode_missingVenue_throwsResourceNotFound() {
        when(venueRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venueService.findByCode("NOPE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue not found: NOPE");
        verify(venueMapper, never()).toResponse(any());
    }

    @Test // FR-SVC-002 + BR-VENUE-002
    void findActiveVenues_returnsOnlyWhatTheActiveQueryReturns() {
        Venue active = venue("VEN-SMR-01", 3, true);
        when(venueRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(active));
        when(venueMapper.toResponse(active)).thenReturn(venueResponse("VEN-SMR-01"));

        List<VenueResponse> result = venueService.findActiveVenues();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("VEN-SMR-01");
        verify(venueRepository, never()).findAll();
    }
}
