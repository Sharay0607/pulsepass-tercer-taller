package com.pulsepass.service;

import com.pulsepass.domain.User;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.pulsepass.service.TestData.user;
import static com.pulsepass.service.TestData.userResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserServiceImpl userService;

    @Test // TEST-USER-001 + BR-USER-003 + BR-USER-004
    void register_validRequest_createsActiveUserWithProfile() {
        // ARRANGE
        UserResponse expected = userResponse("andrea@email.com");
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(expected);

        // ACT
        UserResponse result = userService.register(request("andrea", "Andrea@Email.com", LocalDate.of(2000, 5, 10)));

        // ASSERT
        assertThat(result).isSameAs(expected);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getEmail()).isEqualTo("andrea@email.com");   // normalizado
        assertThat(saved.getProfile()).isNotNull();
        assertThat(saved.getProfile().getUser()).isSameAs(saved);      // misma transacción: cascade User → Profile
        assertThat(saved.getProfile().getFirstName()).isEqualTo("Andrea");
        assertThat(saved.getProfile().getBirthDate()).isEqualTo(LocalDate.of(2000, 5, 10));
    }

    @Test // TEST-USER-002
    void register_duplicateUsername_throwsDuplicateResource() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(request("andrea", "andrea@email.com", LocalDate.of(2000, 1, 1))))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already exists");
        verify(userRepository, never()).save(any());
    }

    @Test // TEST-USER-003 + BR-USER-002 (ignora mayúsculas)
    void register_duplicateEmail_throwsDuplicateResource() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea2")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(request("andrea2", "ANDREA@email.com", LocalDate.of(2000, 1, 1))))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");
        verify(userRepository, never()).save(any());
    }

    @Test // TEST-USER-004
    void register_futureBirthDate_throwsBusinessRule() {
        // ARRANGE
        RegisterUserRequest request = request("andrea", "andrea@email.com", LocalDate.now().plusDays(1));

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Birth date");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_blankUsername_throwsBusinessRule() {
        assertThatThrownBy(() -> userService.register(request("  ", "andrea@email.com", LocalDate.of(2000, 1, 1))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Username");
        verify(userRepository, never()).save(any());
    }

    @Test
    void findByEmail_existingUser_returnsDto() {
        // ARRANGE
        User user = user("andrea@email.com", true, LocalDate.of(2000, 1, 1));
        UserResponse expected = userResponse("andrea@email.com");
        when(userRepository.findByEmailIgnoreCase("ANDREA@email.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        // ACT + ASSERT
        assertThat(userService.findByEmail("ANDREA@email.com")).isSameAs(expected);
    }

    @Test
    void findByEmail_missingUser_throwsResourceNotFound() {
        when(userRepository.findByEmailIgnoreCase("nobody@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("nobody@email.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: nobody@email.com");
    }

    @Test
    void findByUsername_missingUser_throwsResourceNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void findByUsername_existingUser_returnsDto() {
        User user = user("andrea@email.com", true, LocalDate.of(2000, 1, 1));
        UserResponse expected = userResponse("andrea@email.com");
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        assertThat(userService.findByUsername("andrea")).isSameAs(expected);
    }

    private static RegisterUserRequest request(String username, String email, LocalDate birthDate) {
        return new RegisterUserRequest(username, email, "Andrea", "Gómez", "3001234567", "Santa Marta", birthDate);
    }
}
