package com.pulsepass.controller;

import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.pulsepass.controller.ControllerTestData.artist;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
@Import(GlobalExceptionHandler.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    @Test // TEST-CTRL-ART-001
    void shouldReturnArtistById() throws Exception {
        // ARRANGE
        when(artistService.findById(1L)).thenReturn(artist(1L, "Solar Beat"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.active").value(true));

        verify(artistService).findById(1L);
    }

    @Test // TEST-CTRL-ART-002
    void shouldReturn404WhenArtistIdDoesNotExist() throws Exception {
        // ARRANGE
        when(artistService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Artist not found: 99"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test // TEST-CTRL-ART-003
    void shouldReturnArtistByStageName() throws Exception {
        // ARRANGE
        when(artistService.findByStageName("Solar Beat")).thenReturn(artist(1L, "Solar Beat"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findByStageName("Solar Beat");
    }

    @Test // FR-CTRL-ART-002: "retorna 200 o 404"
    void shouldReturn404WhenStageNameDoesNotExist() throws Exception {
        // ARRANGE
        when(artistService.findByStageName("Ghost"))
                .thenThrow(new ResourceNotFoundException("Artist not found: Ghost"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Artist not found: Ghost"));
    }

    @Test // TEST-CTRL-ART-004
    void shouldReturnActiveArtists() throws Exception {
        // ARRANGE
        when(artistService.findActiveArtists()).thenReturn(List.of(
                artist(1L, "Caribbean Sound"),
                artist(2L, "Neon Waves"),
                artist(3L, "Solar Beat")));

        // ACT + ASSERT: "/active" no se confunde con "/{id}"
        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].stageName").value("Caribbean Sound"))
                .andExpect(jsonPath("$[2].stageName").value("Solar Beat"));

        verify(artistService).findActiveArtists();
    }

    @Test // un id no numérico es un request inválido (400), no un error interno (500)
    void shouldReturn400WhenArtistIdIsNotANumber() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.id").exists());

        verify(artistService, never()).findById(any());
    }

    @Test // falta el query param obligatorio → 400, no 500
    void shouldReturn400WhenStageNameParamIsMissing() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/by-stage-name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing request parameter"))
                .andExpect(jsonPath("$.details.stageName").value("Parameter is required"));

        verify(artistService, never()).findByStageName(any());
    }
}
