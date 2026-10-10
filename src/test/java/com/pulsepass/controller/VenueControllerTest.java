package com.pulsepass.controller;

import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.pulsepass.controller.ControllerTestData.venue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VenueController.class)
@Import(GlobalExceptionHandler.class)
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;

    @Test // TEST-CTRL-VEN-001 · AC-CTRL-001
    void shouldReturnVenueByCode() throws Exception {
        // ARRANGE
        when(venueService.findByCode("VEN-SMR-01")).thenReturn(venue("VEN-SMR-01", true));

        // ACT + ASSERT
        mockMvc.perform(get("/api/venues/{code}", "VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.name").value("Marina Convention Center"))
                .andExpect(jsonPath("$.city").value("Santa Marta"))
                .andExpect(jsonPath("$.capacity").value(3))
                .andExpect(jsonPath("$.active").value(true));

        verify(venueService).findByCode("VEN-SMR-01");
    }

    @Test // TEST-CTRL-VEN-002 · AC-CTRL-002
    void shouldReturn404WhenVenueDoesNotExist() throws Exception {
        // ARRANGE
        when(venueService.findByCode("VEN-XXX"))
                .thenThrow(new ResourceNotFoundException("Venue not found: VEN-XXX"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/venues/{code}", "VEN-XXX"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Venue not found: VEN-XXX"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test // TEST-CTRL-VEN-003
    void shouldReturnActiveVenues() throws Exception {
        // ARRANGE
        when(venueService.findActiveVenues()).thenReturn(List.of(
                venue("VEN-SMR-01", true),
                venue("VEN-BOG-01", true)));

        // ACT + ASSERT: "/active" no se confunde con "/{code}"
        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$[1].code").value("VEN-BOG-01"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(venueService).findActiveVenues();
    }

    @Test // NFR-CTRL-007: el 500 no filtra detalles internos
    void shouldReturn500WhenUnexpectedErrorOccurs() throws Exception {
        // ARRANGE
        when(venueService.findByCode("VEN-500"))
                .thenThrow(new IllegalStateException("jdbc password is secret123"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/venues/{code}", "VEN-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("secret123"));
    }

    @Test
    void shouldReturn405WhenHttpMethodIsNotSupported() throws Exception {
        // ACT + ASSERT: no existe DELETE; no debe degradarse a un 500
        mockMvc.perform(delete("/api/venues/{code}", "VEN-SMR-01"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }
}
