package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static com.pulsepass.controller.ControllerTestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    private static final String VALID_BODY = """
            {
              "username": "andrea",
              "email": "andrea@email.com",
              "firstName": "Andrea",
              "lastName": "Gomez",
              "phone": "3001234567",
              "city": "Santa Marta",
              "birthDate": "2000-05-10"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // ------------------------------------------------------------------ POST

    @Test // TEST-CTRL-USR-001 · AC-CTRL-007
    void shouldRegisterUser() throws Exception {
        // ARRANGE
        when(userService.register(any(RegisterUserRequest.class)))
                .thenReturn(user("andrea", "andrea@email.com"));

        // ACT + ASSERT
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.firstName").value("Andrea"));

        // el JSON se convirtió bien al DTO y se delegó al Service
        ArgumentCaptor<RegisterUserRequest> captor = ArgumentCaptor.forClass(RegisterUserRequest.class);
        verify(userService).register(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("andrea");
        assertThat(captor.getValue().email()).isEqualTo("andrea@email.com");
        assertThat(captor.getValue().birthDate()).isEqualTo(LocalDate.of(2000, 5, 10));
    }

    @Test // TEST-CTRL-USR-002
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("andrea@email.com", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").value("Email must be valid"));

        verify(userService, never()).register(any());
    }

    @Test // QT-CTRL-010 / QT-CTRL-007: campos obligatorios ausentes → 400 y el Service no se invoca
    void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.username").value("Username is required"))
                .andExpect(jsonPath("$.details.email").value("Email is required"))
                .andExpect(jsonPath("$.details.firstName").value("First name is required"))
                .andExpect(jsonPath("$.details.lastName").value("Last name is required"))
                .andExpect(jsonPath("$.details.birthDate").value("Birth date is required"));

        verify(userService, never()).register(any());
    }

    @Test
    void shouldReturn400WhenBirthDateHasInvalidFormat() throws Exception {
        // ACT + ASSERT
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("2000-05-10", "not-a-date")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.details.body").exists());

        verify(userService, never()).register(any());
    }

    @Test // TEST-CTRL-USR-003
    void shouldReturn409WhenUsernameIsDuplicated() throws Exception {
        // ARRANGE
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        // ACT + ASSERT
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already exists: andrea"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test // AC-CTRL-008: 409 sin exponer detalles internos
    void shouldReturn409WhenEmailIsDuplicatedWithoutLeakingInternals() throws Exception {
        // ARRANGE
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already exists: andrea@email.com"));

        // ACT + ASSERT
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists: andrea@email.com"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("Exception")
                        .doesNotContain("SQL")
                        .doesNotContain("com.pulsepass"));
    }

    // ------------------------------------------------------------------- GET

    @Test // TEST-CTRL-USR-004
    void shouldReturnUserByEmail() throws Exception {
        // ARRANGE
        when(userService.findByEmail("andrea@email.com")).thenReturn(user("andrea", "andrea@email.com"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByEmail("andrea@email.com");
    }

    @Test // FR-CTRL-USR-002: "retorna 200 o 404"
    void shouldReturn404WhenEmailDoesNotExist() throws Exception {
        // ARRANGE
        when(userService.findByEmail("nobody@email.com"))
                .thenThrow(new ResourceNotFoundException("User not found: nobody@email.com"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/users/by-email").param("email", "nobody@email.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: nobody@email.com"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test // TEST-CTRL-USR-005
    void shouldReturnUserByUsername() throws Exception {
        // ARRANGE
        when(userService.findByUsername("andrea")).thenReturn(user("andrea", "andrea@email.com"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }

    @Test // FR-CTRL-USR-003: "retorna 200 o 404"
    void shouldReturn404WhenUsernameDoesNotExist() throws Exception {
        // ARRANGE
        when(userService.findByUsername("ghost"))
                .thenThrow(new ResourceNotFoundException("User not found: ghost"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/users/by-username").param("username", "ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: ghost"));
    }
}
