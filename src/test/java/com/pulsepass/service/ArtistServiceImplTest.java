package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.service.impl.ArtistServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.pulsepass.service.TestData.artist;
import static com.pulsepass.service.TestData.artistResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock private ArtistRepository artistRepository;
    @Mock private ArtistMapper artistMapper;

    @InjectMocks private ArtistServiceImpl artistService;

    @Test
    void findById_existingArtist_returnsDto() {
        Artist artist = artist(1L, "Solar Beat", true);
        ArtistResponse expected = artistResponse(1L, "Solar Beat");
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(expected);

        assertThat(artistService.findById(1L)).isSameAs(expected);
    }

    @Test // BR-ARTIST-001
    void findById_missingArtist_throwsResourceNotFound() {
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: 99");
        verify(artistMapper, never()).toResponse(any());
    }

    @Test
    void findByStageName_existingArtist_returnsDto() {
        Artist artist = artist(2L, "Neon Waves", true);
        ArtistResponse expected = artistResponse(2L, "Neon Waves");
        when(artistRepository.findByStageNameIgnoreCase("neon waves")).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(expected);

        assertThat(artistService.findByStageName("neon waves")).isSameAs(expected);
    }

    @Test // BR-ARTIST-001
    void findByStageName_missingArtist_throwsResourceNotFound() {
        when(artistRepository.findByStageNameIgnoreCase("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.findByStageName("Ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: Ghost");
    }

    @Test // BR-ARTIST-002
    void findActiveArtists_usesActiveQueryAndMapsResults() {
        Artist a1 = artist(1L, "Caribbean Sound", true);
        Artist a2 = artist(2L, "Neon Waves", true);
        when(artistRepository.findByActiveTrueOrderByStageNameAsc()).thenReturn(List.of(a1, a2));
        // doReturn().when(): evita falsos positivos de strict stubs al stubbear el mismo método con distintos argumentos
        doReturn(artistResponse(1L, "Caribbean Sound")).when(artistMapper).toResponse(a1);
        doReturn(artistResponse(2L, "Neon Waves")).when(artistMapper).toResponse(a2);

        List<ArtistResponse> result = artistService.findActiveArtists();

        assertThat(result).extracting(ArtistResponse::stageName)
                .containsExactly("Caribbean Sound", "Neon Waves");
        verify(artistRepository, never()).findAll();
    }
}
